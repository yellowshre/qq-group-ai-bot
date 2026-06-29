package com.yh.qqbot.chat.history.dto;

public record KnowledgeEmbeddingGenerateResponse(
        long embedded,
        long skipped,
        long failed,
        String status
) {
}
