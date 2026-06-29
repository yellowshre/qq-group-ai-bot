package com.yh.qqbot.chat.history.dto;

public record FormalKnowledgePublishResponse(
        long published,
        long skipped,
        String status
) {
}
