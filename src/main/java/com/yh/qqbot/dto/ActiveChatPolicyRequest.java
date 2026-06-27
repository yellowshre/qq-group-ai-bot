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
        boolean lastMessageFromBot) {
}
