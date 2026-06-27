package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.RouteResult;
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

    public OneBotWsEventHandler(OneBotWsEventParser eventParser, OneBotInboundAdapter inboundAdapter) {
        this.eventParser = eventParser;
        this.inboundAdapter = inboundAdapter;
    }

    public void handle(String payload) {
        OneBotWsEventParser.ParseResult parseResult = eventParser.parseDetailed(payload);
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
}
