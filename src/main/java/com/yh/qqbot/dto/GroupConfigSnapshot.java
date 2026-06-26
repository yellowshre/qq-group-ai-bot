package com.yh.qqbot.dto;

import com.yh.qqbot.enums.MemoryMode;

public record GroupConfigSnapshot(
        String groupId,
        boolean botOn,
        boolean enableChat,
        boolean enableAutoJoin,
        String safeWord,
        String safeWordReply,
        String persona,
        MemoryMode memoryMode
) {

    public GroupConfigSnapshot withBotOn(boolean value) {
        return new GroupConfigSnapshot(groupId, value, enableChat, enableAutoJoin, safeWord, safeWordReply, persona, memoryMode);
    }

    public GroupConfigSnapshot withEnableChat(boolean value) {
        boolean autoJoin = value && enableAutoJoin;
        return new GroupConfigSnapshot(groupId, botOn, value, autoJoin, safeWord, safeWordReply, persona, memoryMode);
    }

    public GroupConfigSnapshot withEnableAutoJoin(boolean value) {
        return new GroupConfigSnapshot(groupId, botOn, enableChat, value, safeWord, safeWordReply, persona, memoryMode);
    }

    public GroupConfigSnapshot withSafeWord(String value) {
        return new GroupConfigSnapshot(groupId, botOn, enableChat, enableAutoJoin, value, safeWordReply, persona, memoryMode);
    }

    public GroupConfigSnapshot withSafeWordReply(String value) {
        return new GroupConfigSnapshot(groupId, botOn, enableChat, enableAutoJoin, safeWord, value, persona, memoryMode);
    }

    public GroupConfigSnapshot withMemoryMode(MemoryMode value) {
        return new GroupConfigSnapshot(groupId, botOn, enableChat, enableAutoJoin, safeWord, safeWordReply, persona, value);
    }
}
