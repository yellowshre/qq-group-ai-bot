package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.OutboundMessage;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.router.MessageRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OneBot 群消息入站适配器。
 *
 * 负责把已经解析好的群消息交给 MessageRouterService 路由，
 * 并根据路由结果调用 QqMessageSender 发送群回复。
 */
@Component
public class OneBotInboundAdapter {

    private static final Logger log = LoggerFactory.getLogger(OneBotInboundAdapter.class);

    /**
     * 群消息路由中枢，负责判断消息应该走表情包、被动聊天、指令还是沉默。
     */
    private final MessageRouterService messageRouterService;

    /**
     * QQ 消息发送接口，负责把最终回复发送回 QQ 群。
     */
    private final QqMessageSender qqMessageSender;

    public OneBotInboundAdapter(MessageRouterService messageRouterService, QqMessageSender qqMessageSender) {
        this.messageRouterService = messageRouterService;
        this.qqMessageSender = qqMessageSender;
    }

    /**
     * 处理一条群消息。
     *
     * 流程：
     * 1. 先交给 MessageRouterService 决定是否回复、回复什么；
     * 2. 如果不需要发送，直接返回路由结果；
     * 3. 如果需要发送，则调用发送器发送群消息；
     * 4. 最后调用 afterSend 做发送后的收尾处理。
     */
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

    /**
     * 发送出站消息。
     *
     * 如果同时包含文本和图片，则拆成两条群消息发送，
     * 避免图文组合在 OneBot 实现中兼容性不稳定。
     */
    private boolean sendOutbound(String groupId, OutboundMessage outboundMessage) {
        if (hasText(outboundMessage) && hasImage(outboundMessage)) {
            boolean textSent = qqMessageSender.sendGroupMessage(groupId, OutboundMessage.text(outboundMessage.text()));
            boolean imageSent = qqMessageSender.sendGroupMessage(groupId, OutboundMessage.image(outboundMessage.imagePath()));
            return textSent && imageSent;
        }
        return qqMessageSender.sendGroupMessage(groupId, outboundMessage);
    }

    /**
     * 判断出站消息是否包含有效文本。
     */
    private boolean hasText(OutboundMessage outboundMessage) {
        return outboundMessage.text() != null && !outboundMessage.text().isBlank();
    }

    /**
     * 判断出站消息是否包含有效图片路径。
     */
    private boolean hasImage(OutboundMessage outboundMessage) {
        return outboundMessage.imagePath() != null && !outboundMessage.imagePath().isBlank();
    }
}