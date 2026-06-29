package com.yh.qqbot.dto;

import com.yh.qqbot.enums.MemoryMode;

public record GroupConfigSnapshot(
        String groupId,
        boolean botOn,
        boolean enableChat,
        boolean enableMeme,
        boolean enablePassiveChat,
        boolean enableAutoJoin,
        long activeCooldownSeconds,
        long activeMaxPerHour,
        long activeMaxPerDay,
        String safeWord,
        String safeWordReply,
        String persona,
        MemoryMode memoryMode,
        boolean enableKnowledgeContext,
        boolean enableMemeKnowledge,
        boolean enablePassiveChatKnowledge,
        boolean enableActiveChatKnowledge
) {

    private static final long DEFAULT_ACTIVE_COOLDOWN_SECONDS = 180;
    private static final long DEFAULT_ACTIVE_MAX_PER_HOUR = 20;
    private static final long DEFAULT_ACTIVE_MAX_PER_DAY = 80;

    public GroupConfigSnapshot(
            String groupId,
            boolean botOn,
            boolean enableChat,
            boolean enableAutoJoin,
            String safeWord,
            String safeWordReply,
            String persona,
            MemoryMode memoryMode) {
        this(
                groupId,
                botOn,
                enableChat,
                true,
                true,
                enableAutoJoin,
                DEFAULT_ACTIVE_COOLDOWN_SECONDS,
                DEFAULT_ACTIVE_MAX_PER_HOUR,
                DEFAULT_ACTIVE_MAX_PER_DAY,
                safeWord,
                safeWordReply,
                persona,
                memoryMode,
                false,
                false,
                false,
                false);
    }

    public GroupConfigSnapshot withBotOn(boolean value) {
        return copy(value, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableChat(boolean value) {
        boolean autoJoin = value && enableAutoJoin;
        return copy(botOn, value, enableMeme, enablePassiveChat, autoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableMeme(boolean value) {
        return copy(botOn, enableChat, value, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnablePassiveChat(boolean value) {
        return copy(botOn, enableChat, enableMeme, value, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableAutoJoin(boolean value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, value,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withActiveCooldownSeconds(long value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                value, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withActiveMaxPerHour(long value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, value, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withActiveMaxPerDay(long value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, value, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withSafeWord(String value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, value, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withSafeWordReply(String value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, value, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withPersona(String value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, value, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withMemoryMode(MemoryMode value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, value,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableKnowledgeContext(boolean value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                value, enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableMemeKnowledge(boolean value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, value, enablePassiveChatKnowledge, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnablePassiveChatKnowledge(boolean value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, value, enableActiveChatKnowledge);
    }

    public GroupConfigSnapshot withEnableActiveChatKnowledge(boolean value) {
        return copy(botOn, enableChat, enableMeme, enablePassiveChat, enableAutoJoin,
                activeCooldownSeconds, activeMaxPerHour, activeMaxPerDay, safeWord, safeWordReply, persona, memoryMode,
                enableKnowledgeContext, enableMemeKnowledge, enablePassiveChatKnowledge, value);
    }

    public boolean passiveChatEnabled() {
        return enableChat && enablePassiveChat;
    }

    public boolean activeChatEnabled() {
        return enableAutoJoin;
    }

    private GroupConfigSnapshot copy(
            boolean newBotOn,
            boolean newEnableChat,
            boolean newEnableMeme,
            boolean newEnablePassiveChat,
            boolean newEnableAutoJoin,
            long newActiveCooldownSeconds,
            long newActiveMaxPerHour,
            long newActiveMaxPerDay,
            String newSafeWord,
            String newSafeWordReply,
            String newPersona,
            MemoryMode newMemoryMode,
            boolean newEnableKnowledgeContext,
            boolean newEnableMemeKnowledge,
            boolean newEnablePassiveChatKnowledge,
            boolean newEnableActiveChatKnowledge) {
        return new GroupConfigSnapshot(
                groupId,
                newBotOn,
                newEnableChat,
                newEnableMeme,
                newEnablePassiveChat,
                newEnableAutoJoin,
                newActiveCooldownSeconds,
                newActiveMaxPerHour,
                newActiveMaxPerDay,
                newSafeWord,
                newSafeWordReply,
                newPersona,
                newMemoryMode,
                newEnableKnowledgeContext,
                newEnableMemeKnowledge,
                newEnablePassiveChatKnowledge,
                newEnableActiveChatKnowledge);
    }
}
