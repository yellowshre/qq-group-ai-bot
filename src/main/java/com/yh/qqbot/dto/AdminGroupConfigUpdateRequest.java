package com.yh.qqbot.dto;

public record AdminGroupConfigUpdateRequest(
        Boolean botOn,
        Boolean enableChat,
        Boolean enableMeme,
        Boolean enablePassiveChat,
        Boolean enableAutoJoin,
        Long activeCooldownSeconds,
        Long activeMaxPerHour,
        Long activeMaxPerDay,
        String safeWord,
        String safeWordReply,
        String persona,
        String memoryMode,
        Boolean enableKnowledgeContext,
        Boolean enableMemeKnowledge,
        Boolean enablePassiveChatKnowledge,
        Boolean enableActiveChatKnowledge
) {
}
