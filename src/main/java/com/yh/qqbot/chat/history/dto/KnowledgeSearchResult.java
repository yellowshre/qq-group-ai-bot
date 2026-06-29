package com.yh.qqbot.chat.history.dto;

public record KnowledgeSearchResult(
        String targetType,
        Long targetId,
        double score,
        String title,
        String content
) {
}
