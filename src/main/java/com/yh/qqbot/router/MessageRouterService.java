package com.yh.qqbot.router;

import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.MemeMatchResult;
import com.yh.qqbot.dto.OutboundMessage;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.enums.RouteType;
import com.yh.qqbot.service.active.ActiveChatDecisionService;
import com.yh.qqbot.service.chat.ChatReplyService;
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

    private final MessageDedupService messageDedupService;
    private final GroupConfigService groupConfigService;
    private final AdminCommandService adminCommandService;
    private final ActiveChatDecisionService activeChatDecisionService;
    private final ChatReplyService chatReplyService;
    private final MemeMatchService memeMatchService;
    private final RateLimitService rateLimitService;
    private final ChatContextService chatContextService;
    private final TriggerLogService triggerLogService;

    public MessageRouterService(
            MessageDedupService messageDedupService,
            GroupConfigService groupConfigService,
            AdminCommandService adminCommandService,
            ActiveChatDecisionService activeChatDecisionService,
            ChatReplyService chatReplyService,
            MemeMatchService memeMatchService,
            RateLimitService rateLimitService,
            ChatContextService chatContextService,
            TriggerLogService triggerLogService) {
        this.messageDedupService = messageDedupService;
        this.groupConfigService = groupConfigService;
        this.adminCommandService = adminCommandService;
        this.activeChatDecisionService = activeChatDecisionService;
        this.chatReplyService = chatReplyService;
        this.memeMatchService = memeMatchService;
        this.rateLimitService = rateLimitService;
        this.chatContextService = chatContextService;
        this.triggerLogService = triggerLogService;
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
            if (!config.enableChat()) {
                return RouteResult.silent("chat switch is off");
            }
            if (!rateLimitService.preConsumePassiveChat(message.groupId())) {
                return RouteResult.silent("passive chat rate limited");
            }
            return buildChatRoute(message, config, RouteType.PASSIVE_CHAT, "passive");
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
        if (success && (result.routeType() == RouteType.PASSIVE_CHAT || result.routeType() == RouteType.ACTIVE_CHAT)) {
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
        MemeMatchResult meme = memeMatchService.match(replyText, message.userId());
        OutboundMessage outbound = meme.matched()
                ? OutboundMessage.textWithImage(replyText, meme.filePath())
                : OutboundMessage.text(replyText);
        return RouteResult.send(routeType, outbound, "chat reply generated")
                .withMemeId(meme.memeId())
                .withMemeHit(meme.matched());
    }

    private RouteResult buildMemeRoute(BotGroupMessage message) {
        if (!rateLimitService.preConsumeEmoji(message.groupId())) {
            return RouteResult.silent("emoji route rate limited");
        }
        MemeMatchResult meme = memeMatchService.match(message.effectiveText(), message.userId());
        if (!meme.matched()) {
            return RouteResult.silent("meme not matched");
        }
        return RouteResult.send(RouteType.MEME, OutboundMessage.image(meme.filePath()), "meme matched")
                .withMemeId(meme.memeId())
                .withMemeHit(true);
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
