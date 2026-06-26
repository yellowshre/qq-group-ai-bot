package com.yh.qqbot.dto;

public record CommandHandleResult(boolean handled, OutboundMessage outboundMessage, String operation, String detail) {

    public static CommandHandleResult notCommand() {
        return new CommandHandleResult(false, null, null, null);
    }

    public static CommandHandleResult handled(String operation, String detail, String replyText) {
        return new CommandHandleResult(true, OutboundMessage.text(replyText), operation, detail);
    }
}
