package com.yh.qqbot.dto;

public record ActiveChatPolicyRequest(
        Long groupId,
        Long userId,
        String rawMessage,
        boolean atBot,
        boolean botNicknameMatched,
        boolean groupBotEnabled,
        boolean activeChatEnabledInGroup,
        boolean adminCommandHit,
        boolean memeAlreadySent,
        boolean lastMessageFromBot,
        Long cooldownSeconds,
        Long maxPerHour,
        Long maxPerDay) {

    public ActiveChatPolicyRequest(
            Long groupId,
            Long userId,
            String rawMessage,
            boolean atBot,
            boolean botNicknameMatched,
            boolean groupBotEnabled,
            boolean activeChatEnabledInGroup,
            boolean adminCommandHit,
            boolean memeAlreadySent,
            boolean lastMessageFromBot) {
        this(
                groupId,
                userId,
                rawMessage,
                atBot,
                botNicknameMatched,
                groupBotEnabled,
                activeChatEnabledInGroup,
                adminCommandHit,
                memeAlreadySent,
                lastMessageFromBot,
                null,
                null,
                null);
    }
}
