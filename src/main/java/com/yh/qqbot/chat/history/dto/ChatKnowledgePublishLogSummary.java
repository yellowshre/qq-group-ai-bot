package com.yh.qqbot.chat.history.dto;

import com.yh.qqbot.chat.history.entity.ChatKnowledgePublishLogEntity;
import java.time.LocalDateTime;

public record ChatKnowledgePublishLogSummary(
        Long id,
        String sourceType,
        Long sourceId,
        String targetType,
        Long targetId,
        String action,
        String operator,
        String comment,
        LocalDateTime createdAt) {

    public static ChatKnowledgePublishLogSummary from(ChatKnowledgePublishLogEntity entity) {
        return new ChatKnowledgePublishLogSummary(
                entity.getId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getAction(),
                entity.getOperator(),
                entity.getComment(),
                entity.getCreatedAt());
    }
}
