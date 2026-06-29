package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.OutboundMessage;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
@ConditionalOnProperty(prefix = "qqbot.onebot.ws", name = "enabled", havingValue = "true")
public class OneBotWsMessageSender implements QqMessageSender {

    private static final Logger log = LoggerFactory.getLogger(OneBotWsMessageSender.class);

    private final OneBotWebSocketClient webSocketClient;
    private final OneBotWsActionFactory actionFactory;

    public OneBotWsMessageSender(OneBotWebSocketClient webSocketClient, OneBotWsActionFactory actionFactory) {
        this.webSocketClient = webSocketClient;
        this.actionFactory = actionFactory;
    }

    @PostConstruct
    public void logSenderType() {
        log.info("QQ message sender active: OneBotWsMessageSender");
    }

    @Override
    public boolean sendGroupMessage(String groupId, OutboundMessage outboundMessage) {
        if (outboundMessage == null || outboundMessage.isEmpty()) {
            return false;
        }
        String echo = "qqbot-ws-" + UUID.randomUUID();
        Map<String, Object> action = actionFactory.sendGroupMessage(groupId, outboundMessage, echo);
        boolean sent = webSocketClient.sendAction(action);
        if (!sent) {
            log.warn("OneBot WebSocket send_group_msg failed before send. groupId={}, echo={}", groupId, echo);
        }
        return sent;
    }

    @Override
    public boolean sendPrivateMessage(String userId, OutboundMessage outboundMessage) {
        if (outboundMessage == null || outboundMessage.isEmpty()) {
            return false;
        }
        String echo = "qqbot-ws-private-" + UUID.randomUUID();
        Map<String, Object> action = actionFactory.sendPrivateMessage(userId, outboundMessage, echo);
        boolean sent = webSocketClient.sendAction(action);
        if (!sent) {
            log.warn("OneBot WebSocket send_private_msg failed before send. userId={}, echo={}", userId, echo);
        }
        return sent;
    }
}
