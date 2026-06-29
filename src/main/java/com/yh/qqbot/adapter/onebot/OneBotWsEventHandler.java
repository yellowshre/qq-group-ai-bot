package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.BotPrivateMessage;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.service.command.PrivateAdminCommandService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "qqbot.onebot.ws", name = "enabled", havingValue = "true")
public class OneBotWsEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OneBotWsEventHandler.class);

    private final OneBotWsEventParser eventParser;
    private final OneBotInboundAdapter inboundAdapter;
    private final PrivateAdminCommandService privateAdminCommandService;
    private final QqMessageSender qqMessageSender;

    public OneBotWsEventHandler(
            OneBotWsEventParser eventParser,
            OneBotInboundAdapter inboundAdapter,
            PrivateAdminCommandService privateAdminCommandService,
            QqMessageSender qqMessageSender) {
        this.eventParser = eventParser;
        this.inboundAdapter = inboundAdapter;
        this.privateAdminCommandService = privateAdminCommandService;
        this.qqMessageSender = qqMessageSender;
    }

    public void handle(String payload) {
        OneBotWsEventParser.ParseResult parseResult = eventParser.parseDetailed(payload);
        Optional<BotPrivateMessage> privateMessage = parseResult.privateMessage();
        if (privateMessage.isPresent()) {
            handlePrivateMessage(privateMessage.get());
            return;
        }

        Optional<BotGroupMessage> message = parseResult.message();
        if (message.isEmpty()) {
            log.debug("Ignore OneBot WebSocket payload. reason={}, post_type={}, message_type={}, group_id={}, user_id={}, raw_message={}",
                    parseResult.ignoredReason(), parseResult.postType(), parseResult.messageType(),
                    parseResult.groupId(), parseResult.userId(), parseResult.rawMessage());
            return;
        }
        BotGroupMessage groupMessage = message.get();
        log.info("Route OneBot group message start. groupId={}, userId={}, messageId={}, rawMessage={}, routedText={}, mentionedSelf={}",
                groupMessage.groupId(), groupMessage.userId(), groupMessage.messageId(),
                groupMessage.rawMessage(), groupMessage.effectiveText(), groupMessage.atBot());
        RouteResult result = inboundAdapter.handleGroupMessage(groupMessage);
        log.info("Route OneBot group message done. routeType={}, responseType={}, shouldSend={}, silentReason={}, passiveChatHit={}, memeHit={}, activeChatHit={}",
                result.routeType(), result.responseType(), result.shouldSend(), result.silentReason(),
                result.passiveChatHit(), result.memeHit(), result.activeChatHit());
    }

    private void handlePrivateMessage(BotPrivateMessage message) {
        CommandHandleResult result = privateAdminCommandService.tryHandle(message);
        if (!result.handled()) {
            log.debug("Ignore OneBot private message. userId={}, messageId={}, reason=NOT_PRIVATE_ADMIN_COMMAND",
                    message.userId(), message.messageId());
            return;
        }
        if (result.outboundMessage() == null || result.outboundMessage().isEmpty()) {
            log.debug("Private admin command handled without outbound. userId={}, messageId={}, operation={}",
                    message.userId(), message.messageId(), result.operation());
            return;
        }
        boolean sent = qqMessageSender.sendPrivateMessage(message.userId(), result.outboundMessage());
        log.info("Private admin command handled. userId={}, messageId={}, operation={}, sent={}",
                message.userId(), message.messageId(), result.operation(), sent);
    }
}
