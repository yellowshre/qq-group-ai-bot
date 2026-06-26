package com.yh.qqbot.router;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.MemeMatchResult;
import com.yh.qqbot.dto.OutboundMessage;
import com.yh.qqbot.dto.PassiveChatReply;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.enums.RouteType;
import com.yh.qqbot.service.active.ActiveChatDecisionService;
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
import org.springframework.stereotype.Service;

@Service
public class MessageRouterService {

    private static final String PASSIVE_DIFY_CHAT = "PASSIVE_DIFY_CHAT";
    private static final String DEFAULT_BOT_NAME = "\u5c0f\u9ec4";

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
            if (message.effectiveText().toLowerCase(java.util.Locale.ROOT).startsWith("#boton")) {
                CommandHandleResult command = adminCommandService.tryHandle(message, config);
                if (command.handled() && "BOT_ON".equals(command.operation())) {
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

        Optional<RouteResult> safeWordResult = handleSafeWord(message, config);
        if (safeWordResult.isPresent()) {
            return safeWordResult.get();
        }

        chatContextService.rememberRecentMessage(message.groupId(), message.effectiveText());

        if (message.triggersPassiveChat()) {
            return handlePassiveChat(message, config);
        }

        if (config.enableAutoJoin() && activeChatDecisionService.shouldJoin(message, config)) {
            if (!rateLimitService.preConsumeActiveChat(message.groupId())) {
                return RouteResult.silent("active chat rate limited");
            }
            return buildChatRoute(message, config, RouteType.ACTIVE_CHAT, "active");
        }

        return buildMemeRoute(message);
    }

    public void afterSend(BotGroupMessage message, RouteResult result, boolean success) {
        if (success && result.routeType() == RouteType.PASSIVE_CHAT && PASSIVE_DIFY_CHAT.equals(result.workflowType())) {
            chatContextService.appendUserMessage(parseLong(message.groupId()), parseLong(message.userId()), message.effectiveText());
            chatContextService.appendBotReply(parseLong(message.groupId()), botName(), result.replyText());
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

    private RouteResult handlePassiveChat(BotGroupMessage message, GroupConfigSnapshot config) {
        if (!config.enableChat()) {
            return passiveSilent("chat switch is off");
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

    private String botName() {
        return properties.getNicknames().stream()
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .map(String::strip)
                .orElse(DEFAULT_BOT_NAME);
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
