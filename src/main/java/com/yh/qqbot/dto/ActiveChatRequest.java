package com.yh.qqbot.dto;

public record ActiveChatRequest(
        String text,
        Long groupId,
        Long userId,
        String botName,
        String persona,
        String recentMessages,
        String activeReason,
        String riskHint) {
}
