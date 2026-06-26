package com.yh.qqbot.dto;

import java.time.Instant;

public record BotGroupMessage(
        String groupId,
        String userId,
        String messageId,
        String rawMessage,
        String plainText,
        boolean atBot,
        boolean mentionedBotNickname,
        Instant receivedAt
) {

    public boolean triggersPassiveChat() {
        return atBot || mentionedBotNickname;
    }

    public String effectiveText() {
        return plainText == null ? "" : plainText.strip();
    }
}
