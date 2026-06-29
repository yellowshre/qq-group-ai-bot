package com.yh.qqbot.chat.history.dto;

public record KnowledgeContextItem(
        String targetType,
        Long targetId,
        String type,
        String title,
        String content,
        double score,
        String usageHint
) {
}
