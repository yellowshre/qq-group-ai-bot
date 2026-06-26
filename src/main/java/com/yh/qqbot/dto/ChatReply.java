package com.yh.qqbot.dto;

public record ChatReply(String replyText, String sceneCode) {

    public boolean hasText() {
        return replyText != null && !replyText.isBlank();
    }
}
