package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.OutboundMessage;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.router.MessageRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OneBotInboundAdapter {

    private static final Logger log = LoggerFactory.getLogger(OneBotInboundAdapter.class);

    private final MessageRouterService messageRouterService;
    private final QqMessageSender qqMessageSender;

    public OneBotInboundAdapter(MessageRouterService messageRouterService, QqMessageSender qqMessageSender) {
        this.messageRouterService = messageRouterService;
        this.qqMessageSender = qqMessageSender;
    }

    public RouteResult handleGroupMessage(BotGroupMessage message) {
        RouteResult result = messageRouterService.route(message);
        if (!result.shouldSend() || result.outboundMessage() == null || result.outboundMessage().isEmpty()) {
            return result;
        }

        boolean success = false;
        try {
            success = sendOutbound(message.groupId(), result.outboundMessage());
            return result;
        } catch (Exception ex) {
            log.warn("Failed to send group message. groupId={}, messageId={}", message.groupId(), message.messageId(), ex);
            return result;
        } finally {
            messageRouterService.afterSend(message, result, success);
        }
    }

    private boolean sendOutbound(String groupId, OutboundMessage outboundMessage) {
        if (hasText(outboundMessage) && hasImage(outboundMessage)) {
            boolean textSent = qqMessageSender.sendGroupMessage(groupId, OutboundMessage.text(outboundMessage.text()));
            boolean imageSent = qqMessageSender.sendGroupMessage(groupId, OutboundMessage.image(outboundMessage.imagePath()));
            return textSent && imageSent;
        }
        return qqMessageSender.sendGroupMessage(groupId, outboundMessage);
    }

    private boolean hasText(OutboundMessage outboundMessage) {
        return outboundMessage.text() != null && !outboundMessage.text().isBlank();
    }

    private boolean hasImage(OutboundMessage outboundMessage) {
        return outboundMessage.imagePath() != null && !outboundMessage.imagePath().isBlank();
    }
}
