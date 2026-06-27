package com.yh.qqbot.router;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ActiveChatPolicyRequest;
import com.yh.qqbot.dto.ActiveChatPolicyResult;
import com.yh.qqbot.dto.ActiveChatReplyResult;
import com.yh.qqbot.dto.ActiveChatRequest;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.MemeMatchResult;
import com.yh.qqbot.dto.OutboundMessage;
import com.yh.qqbot.dto.PassiveChatReply;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.dto.SafetyWordMatchResult;
import com.yh.qqbot.enums.RouteType;
import com.yh.qqbot.service.active.ActiveChatDecisionService;
import com.yh.qqbot.service.active.ActiveChatPolicyService;
import com.yh.qqbot.service.bot.BotIdentityService;
import com.yh.qqbot.service.bot.BotSafetyWordService;
import com.yh.qqbot.service.chat.ChatReplyService;
import com.yh.qqbot.service.chat.DifyWorkflowService;
import com.yh.qqbot.service.chat.GroupPersonaService;
import com.yh.qqbot.service.command.AdminCommandService;
import com.yh.qqbot.service.config.GroupConfigService;
import com.yh.qqbot.service.context.ChatContextService;
import com.yh.qqbot.service.log.TriggerLogService;
import com.yh.qqbot.service.meme.MemeMatchService;
import com.yh.qqbot.service.rate.MessageDedupService;
import com.yh.qqbot.service.rate.RateLimitService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageRouterService {

    private static final Logger log = LoggerFactory.getLogger(MessageRouterService.class);
    private static final String PASSIVE_DIFY_CHAT = "PASSIVE_DIFY_CHAT";
    private static final String ACTIVE_DIFY_CHAT = "ACTIVE_DIFY_CHAT";
    private static final String ACTIVE_POLICY_UNAVAILABLE = "ACTIVE_POLICY_UNAVAILABLE";
    private static final String DIFY_DISABLED = "DIFY_DISABLED";
    private static final String PASSIVE_CHAT_API_KEY_EMPTY = "PASSIVE_CHAT_API_KEY_EMPTY";
    private static final String PASSIVE_CHAT_WORKFLOW_EMPTY = "PASSIVE_CHAT_WORKFLOW_EMPTY";
    private static final String PASSIVE_CHAT_REPLY_EMPTY_OR_INVALID = "PASSIVE_CHAT_REPLY_EMPTY_OR_INVALID";
    private static final String DEFAULT_BOT_NAME = "\u5c0f\u9ec4";
    private static final String DEFAULT_RISK_HINT = "\u65e0\u660e\u663e\u98ce\u9669";

    private final MessageDedupService messageDedupService;
    private final GroupConfigService groupConfigService;
    private final AdminCommandService adminCommandService;
    private final ActiveChatDecisionService activeChatDecisionService;
    private final ChatReplyService chatReplyService;
    private final MemeMatchService memeMatchService;
    private final RateLimitService rateLimitService;
    private final ChatContextService chatContextService;
    private final TriggerLogService triggerLogService;
    private final GroupPersonaService groupPersonaService;
    private final DifyWorkflowService difyWorkflowService;
    private final ActiveChatPolicyService activeChatPolicyService;
    private final BotIdentityService botIdentityService;
    private final BotSafetyWordService botSafetyWordService;
    private final QqBotProperties properties;

    public MessageRouterService(
            MessageDedupService messageDedupService,
            GroupConfigService groupConfigService,
            AdminCommandService adminCommandService,
            ActiveChatDecisionService activeChatDecisionService,
            ChatReplyService chatReplyService,
            MemeMatchService memeMatchService,
            RateLimitService rateLimitService,
            ChatContextService chatContextService,
            TriggerLogService triggerLogService,
            GroupPersonaService groupPersonaService,
            DifyWorkflowService difyWorkflowService,
            ActiveChatPolicyService activeChatPolicyService,
            BotIdentityService botIdentityService,
            BotSafetyWordService botSafetyWordService,
            QqBotProperties properties) {
        this.messageDedupService = messageDedupService;
        this.groupConfigService = groupConfigService;
        this.adminCommandService = adminCommandService;
        this.activeChatDecisionService = activeChatDecisionService;
        this.chatReplyService = chatReplyService;
        this.memeMatchService = memeMatchService;
        this.rateLimitService = rateLimitService;
        this.chatContextService = chatContextService;
        this.triggerLogService = triggerLogService;
        this.groupPersonaService = groupPersonaService;
        this.difyWorkflowService = difyWorkflowService;
        this.activeChatPolicyService = activeChatPolicyService;
        this.botIdentityService = botIdentityService;
        this.botSafetyWordService = botSafetyWordService;
        this.properties = properties;
    }

    public RouteResult route(BotGroupMessage message) {
        long startedAt = System.nanoTime();
        return doRoute(message).withDurationMs(elapsedMs(startedAt));
    }

    private RouteResult doRoute(BotGroupMessage message) {
        if (!messageDedupService.firstSeen(message)) {
            return RouteResult.silent("duplicate or dedup unavailable").withDedupPassed(false);
        }

        GroupConfigSnapshot config = groupConfigService.getConfig(message.groupId());
        if (!config.botOn()) {
            String text = message.effectiveText().toLowerCase(java.util.Locale.ROOT);
            if (text.startsWith("#boton")
                    || text.startsWith("\u0023\u673a\u5668\u4eba\u6062\u590d")
                    || text.startsWith("\u0023\u72b6\u6001")) {
                CommandHandleResult command = adminCommandService.tryHandle(message, config);
                if (command.handled()) {
                    return RouteResult.send(RouteType.COMMAND, command.outboundMessage(), command.operation())
                            .withAdminCommandHit(true);
                }
            }
            return RouteResult.silent("group bot switch is off");
        }

        CommandHandleResult command = adminCommandService.tryHandle(message, config);
        if (command.handled()) {
            return RouteResult.send(RouteType.COMMAND, command.outboundMessage(), command.operation())
                    .withAdminCommandHit(true);
        }

        Optional<RouteResult> configuredSafetyWordResult = handleConfiguredSafetyWord(message, config);
        if (configuredSafetyWordResult.isPresent()) {
            return configuredSafetyWordResult.get();
        }

        Optional<RouteResult> safeWordResult = handleSafeWord(message, config);
        if (safeWordResult.isPresent()) {
            return safeWordResult.get();
        }

        chatContextService.rememberRecentMessage(message.groupId(), message.effectiveText());

        if (message.triggersPassiveChat() || botIdentityService.isPassiveTriggerMatched(message.effectiveText())) {
            return handlePassiveChat(message, config);
        }

        RouteResult memeResult = buildMemeRoute(message);
        if (memeResult.shouldSend()) {
            return memeResult;
        }

        return handleActiveChat(message, config, memeResult);
    }

    public void afterSend(BotGroupMessage message, RouteResult result, boolean success) {
        if (success && result.routeType() == RouteType.PASSIVE_CHAT && PASSIVE_DIFY_CHAT.equals(result.workflowType())) {
            chatContextService.appendUserMessage(parseLong(message.groupId()), parseLong(message.userId()), message.effectiveText());
            chatContextService.appendBotReply(parseLong(message.groupId()), botName(), result.replyText());
        } else if (success && result.routeType() == RouteType.ACTIVE_CHAT && ACTIVE_DIFY_CHAT.equals(result.workflowType())) {
            Long groupId = parseLong(message.groupId());
            chatContextService.appendUserMessage(groupId, parseLong(message.userId()), message.effectiveText());
            chatContextService.appendBotReply(groupId, botName(), result.replyText());
            activeChatPolicyService.markActiveChatSent(groupId, result.cooldownSeconds());
        } else if (success && (result.routeType() == RouteType.PASSIVE_CHAT || result.routeType() == RouteType.ACTIVE_CHAT)) {
            String replyText = result.outboundMessage() == null ? "" : result.outboundMessage().text();
            GroupConfigSnapshot config = groupConfigService.getConfig(message.groupId());
            chatContextService.appendTurn(message.groupId(), message.effectiveText(), replyText, config.memoryMode());
        }
        if (result.routeType() != RouteType.SILENT) {
            triggerLogService.record(message, result, success, success ? null : "send failed");
        }
    }

    private Optional<RouteResult> handleSafeWord(BotGroupMessage message, GroupConfigSnapshot config) {
        String safeWord = config.safeWord();
        if (safeWord == null || safeWord.isBlank() || !message.effectiveText().contains(safeWord)) {
            return Optional.empty();
        }
        groupConfigService.updateConfig(message.groupId(), snapshot -> snapshot.withEnableAutoJoin(false));
        return Optional.of(RouteResult.send(
                RouteType.SAFE_WORD,
                OutboundMessage.text(config.safeWordReply()),
                "safe word triggered"
        ));
    }

    private Optional<RouteResult> handleConfiguredSafetyWord(BotGroupMessage message, GroupConfigSnapshot config) {
        SafetyWordMatchResult safetyWord = botSafetyWordService.match(message.effectiveText());
        if (!safetyWord.matched()) {
            return Optional.empty();
        }
        if (safetyWord.adminOnly() && !isAdmin(message.userId())) {
            return Optional.of(RouteResult.silent("safety word admin only")
                    .withAdminCommandHit(true)
                    .withActivePolicyPassed(false)
                    .withActivePolicyRejectReason(ActiveChatPolicyResult.ADMIN_COMMAND)
                    .withSilentReason("safety word admin only"));
        }

        if (SafetyWordMatchResult.ACTIVE_CHAT_OFF.equals(safetyWord.action())) {
            groupConfigService.updateConfig(message.groupId(), snapshot -> snapshot.withEnableAutoJoin(false));
            return Optional.of(RouteResult.send(
                    RouteType.COMMAND,
                    OutboundMessage.text("active chat off"),
                    "ACTIVE_CHAT_OFF"
            ).withAdminCommandHit(true));
        }
        if (SafetyWordMatchResult.ACTIVE_CHAT_ON.equals(safetyWord.action())) {
            if (!config.enableChat()) {
                return Optional.of(RouteResult.send(
                        RouteType.COMMAND,
                        OutboundMessage.text("please enable chat first"),
                        "ACTIVE_CHAT_ON_REJECTED"
                ).withAdminCommandHit(true));
            }
            groupConfigService.updateConfig(message.groupId(), snapshot -> snapshot.withEnableAutoJoin(true));
            return Optional.of(RouteResult.send(
                    RouteType.COMMAND,
                    OutboundMessage.text("active chat on"),
                    "ACTIVE_CHAT_ON"
            ).withAdminCommandHit(true));
        }
        return Optional.empty();
    }

    private RouteResult handlePassiveChat(BotGroupMessage message, GroupConfigSnapshot config) {
        if (!config.enableChat()) {
            return passiveSilent("chat switch is off");
        }
        if (!config.enablePassiveChat()) {
            return passiveSilent("passive chat switch is off");
        }
        if (!rateLimitService.preConsumePassiveChat(message.groupId())) {
            return passiveSilent("passive chat rate limited");
        }

        Long groupId = parseLong(message.groupId());
        Long userId = parseLong(message.userId());
        String botName = botName();
        String persona = groupPersonaService.getPersona(groupId);
        String recentMessages = chatContextService.getRecentMessages(groupId);

        Optional<PassiveChatReply> reply = difyWorkflowService.generatePassiveReply(
                message.effectiveText(),
                groupId,
                userId,
                botName,
                persona,
                java.util.List.of(recentMessages));
        if (reply.isEmpty()) {
            String unavailableReason = passiveChatUnavailableReason();
            log.warn("Passive chat unavailable. reason={}, difyEnabled={}, baseUrlConfigured={}, passiveChatApiKeyConfigured={}",
                    unavailableReason,
                    properties.getDify().isEnabled(),
                    hasText(properties.getDify().getBaseUrl()),
                    hasText(properties.getDify().getPassiveChatApiKey()));
            return passiveSilent(properties.getDify().isEnabled() ? "passive chat unavailable" : "dify disabled");
        }

        PassiveChatReply passiveReply = reply.get();
        String replyText = passiveReply.replyText() == null ? "" : passiveReply.replyText().strip();
        if (replyText.isBlank()) {
            return passiveSilent("passive reply empty")
                    .withChatConfidence(passiveReply.confidence());
        }
        if (passiveReply.confidence() < properties.getDify().getPassiveChatMinConfidence()) {
            return passiveSilent("passive confidence low")
                    .withReplyText(replyText)
                    .withChatConfidence(passiveReply.confidence());
        }

        MemeMatchResult meme = matchReplyMeme(replyText, message);
        OutboundMessage outbound = meme.matched()
                ? OutboundMessage.textWithImage(replyText, meme.filePath())
                : OutboundMessage.text(replyText);
        RouteResult result = RouteResult.send(RouteType.PASSIVE_CHAT, outbound, "passive chat reply generated")
                .withPassiveChatHit(true)
                .withReplyText(replyText)
                .withChatConfidence(passiveReply.confidence())
                .withMemeHit(meme.matched())
                .withWorkflowType(PASSIVE_DIFY_CHAT);
        if (meme.matched()) {
            result = result.withMemeMetadata(meme).withWorkflowType(PASSIVE_DIFY_CHAT);
        }
        return result;
    }

    private RouteResult buildChatRoute(
            BotGroupMessage message,
            GroupConfigSnapshot config,
            RouteType routeType,
            String triggerType) {
        Optional<ChatReply> reply = chatReplyService.generate(message, config, triggerType);
        if (reply.isEmpty()) {
            return RouteResult.silent("chat workflow returned empty");
        }
        String replyText = reply.get().replyText();
        MemeMatchResult meme = memeMatchService.match(replyText, message.groupId(), message.userId());
        OutboundMessage outbound = meme.matched()
                ? OutboundMessage.textWithImage(replyText, meme.filePath())
                : OutboundMessage.text(replyText);
        return RouteResult.send(routeType, outbound, "chat reply generated")
                .withMemeHit(meme.matched())
                .withMemeMetadata(meme);
    }

    private RouteResult buildMemeRoute(BotGroupMessage message) {
        GroupConfigSnapshot config = groupConfigService.getConfig(message.groupId());
        if (!config.enableMeme()) {
            return RouteResult.silent("meme switch is off")
                    .withSilentReason("meme switch is off");
        }
        if (!rateLimitService.preConsumeEmoji(message.groupId())) {
            return RouteResult.silent("emoji route rate limited");
        }
        MemeMatchResult meme = memeMatchService.match(message.effectiveText(), message.groupId(), message.userId());
        if (!meme.matched()) {
            return RouteResult.silent(meme.missReason() == null ? "meme not matched" : meme.missReason())
                    .withMemeMetadata(meme);
        }
        return RouteResult.send(RouteType.MEME, OutboundMessage.image(meme.filePath()), "meme matched")
                .withMemeHit(true)
                .withMemeMetadata(meme);
    }

    private RouteResult handleActiveChat(BotGroupMessage message, GroupConfigSnapshot config, RouteResult memeResult) {
        ActiveChatPolicyRequest policyRequest = new ActiveChatPolicyRequest(
                parseLong(message.groupId()),
                parseLong(message.userId()),
                message.effectiveText(),
                message.atBot(),
                message.mentionedBotNickname()
                        || botIdentityService.isBotAliasMatched(message.effectiveText())
                        || botIdentityService.isPassiveTriggerMatched(message.effectiveText()),
                config.botOn(),
                config.activeChatEnabled(),
                false,
                memeResult != null && memeResult.shouldSend(),
                false,
                config.activeCooldownSeconds(),
                config.activeMaxPerHour(),
                config.activeMaxPerDay()
        );
        ActiveChatPolicyResult policy;
        try {
            policy = activeChatPolicyService.evaluate(policyRequest);
        } catch (Exception ex) {
            policy = null;
        }
        if (policy == null) {
            return RouteResult.silent(ACTIVE_POLICY_UNAVAILABLE)
                    .withActiveChatHit(false)
                    .withActivePolicyPassed(false)
                    .withActivePolicyRejectReason(ACTIVE_POLICY_UNAVAILABLE)
                    .withSilentReason(ACTIVE_POLICY_UNAVAILABLE);
        }
        if (!policy.allowed()) {
            return RouteResult.silent(policy.rejectReason())
                    .withActiveChatHit(false)
                    .withActivePolicy(policy)
                    .withSilentReason(policy.rejectReason());
        }

        ActiveChatRequest activeRequest = new ActiveChatRequest(
                message.effectiveText(),
                parseLong(message.groupId()),
                parseLong(message.userId()),
                botName(),
                groupPersonaService.getPersona(parseLong(message.groupId())),
                chatContextService.getRecentMessages(parseLong(message.groupId())),
                policy.reason(),
                DEFAULT_RISK_HINT
        );
        ActiveChatReplyResult reply;
        try {
            reply = difyWorkflowService.generateActiveReply(activeRequest);
        } catch (Exception ex) {
            reply = ActiveChatReplyResult.rejected(ActiveChatReplyResult.DIFY_ERROR);
        }
        if (reply == null || !reply.success() || !reply.shouldReply()) {
            String rejectReason = reply == null ? ActiveChatReplyResult.DIFY_ERROR : reply.rejectReason();
            RouteResult result = RouteResult.silent(rejectReason)
                    .withActiveChatHit(true)
                    .withActivePolicy(policy)
                    .withActivePolicyPassed(true)
                    .withActiveShouldReply(false)
                    .withWorkflowType(ACTIVE_DIFY_CHAT)
                    .withSilentReason(rejectReason);
            if (reply != null) {
                result = result.withActiveReply(reply);
            }
            return result;
        }

        String replyText = reply.replyText() == null ? "" : reply.replyText().strip();
        if (replyText.isBlank() || reply.confidence() < properties.getActiveChat().getMinConfidence()) {
            String rejectReason = replyText.isBlank()
                    ? ActiveChatReplyResult.EMPTY_REPLY
                    : ActiveChatReplyResult.LOW_CONFIDENCE;
            return RouteResult.silent(rejectReason)
                    .withActiveChatHit(true)
                    .withActivePolicy(policy)
                    .withActivePolicyPassed(true)
                    .withActiveShouldReply(false)
                    .withActiveConfidence(reply.confidence())
                    .withWorkflowType(ACTIVE_DIFY_CHAT)
                    .withReplyText(replyText)
                    .withSilentReason(rejectReason);
        }

        return RouteResult.send(RouteType.ACTIVE_CHAT, OutboundMessage.text(replyText), "active chat reply generated")
                .withActiveChatHit(true)
                .withActivePolicy(policy)
                .withActivePolicyPassed(true)
                .withActiveShouldReply(true)
                .withActiveConfidence(reply.confidence())
                .withWorkflowType(ACTIVE_DIFY_CHAT)
                .withReplyText(replyText);
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private MemeMatchResult matchReplyMeme(String replyText, BotGroupMessage message) {
        try {
            return memeMatchService.match(replyText, message.groupId(), message.userId());
        } catch (Exception ex) {
            return MemeMatchResult.empty("reply meme match failed");
        }
    }

    private RouteResult passiveSilent(String reason) {
        return RouteResult.silent(reason)
                .withPassiveChatHit(true)
                .withWorkflowType(PASSIVE_DIFY_CHAT)
                .withSilentReason(reason);
    }

    private String passiveChatUnavailableReason() {
        QqBotProperties.Dify dify = properties.getDify();
        if (!dify.isEnabled()) {
            return DIFY_DISABLED;
        }
        if (!hasText(dify.getPassiveChatApiKey())) {
            return PASSIVE_CHAT_API_KEY_EMPTY;
        }
        if (!hasText(dify.getPassiveChatWorkflowId())) {
            return PASSIVE_CHAT_WORKFLOW_EMPTY;
        }
        return PASSIVE_CHAT_REPLY_EMPTY_OR_INVALID;
    }

    private String botName() {
        String displayName = botIdentityService.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName.strip();
        }
        return properties.getNicknames().stream()
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .map(String::strip)
                .orElse(DEFAULT_BOT_NAME);
    }

    private boolean isAdmin(String userId) {
        return properties.getAdmins().stream()
                .filter(admin -> admin != null && !admin.isBlank())
                .anyMatch(admin -> admin.equals(userId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
