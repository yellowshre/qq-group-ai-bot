package com.yh.qqbot.dto;

public record DevPrivateMessageResult(
        String userId,
        String messageId,
        boolean handled,
        boolean shouldReply,
        String operation,
        String detail,
        OutboundMessage outboundMessage
) {
    public static DevPrivateMessageResult from(String userId, String messageId, CommandHandleResult result) {
        OutboundMessage outboundMessage = result == null ? null : result.outboundMessage();
        boolean handled = result != null && result.handled();
        boolean shouldReply = handled && outboundMessage != null && !outboundMessage.isEmpty();
        return new DevPrivateMessageResult(
                userId,
                messageId,
                handled,
                shouldReply,
                result == null ? null : result.operation(),
                result == null ? null : result.detail(),
                outboundMessage);
    }
}
