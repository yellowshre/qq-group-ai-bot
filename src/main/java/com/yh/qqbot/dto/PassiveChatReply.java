package com.yh.qqbot.dto;

public record PassiveChatReply(String replyText, double confidence) {

    public boolean hasText() {
        return replyText != null && !replyText.isBlank();
    }
}
