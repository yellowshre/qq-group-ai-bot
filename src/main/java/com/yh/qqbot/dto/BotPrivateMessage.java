package com.yh.qqbot.dto;

import java.time.Instant;

public record BotPrivateMessage(
        String userId,
        String messageId,
        String rawMessage,
        String effectiveText,
        Instant receivedAt
) {
}
