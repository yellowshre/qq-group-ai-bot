package com.yh.qqbot.chat.history.dto;

import com.yh.qqbot.chat.history.entity.ChatKnowledgeEmbeddingEntity;
import java.time.LocalDateTime;

public record ChatKnowledgeEmbeddingSummary(
        Long id,
        String groupId,
        String targetType,
        Long targetId,
        String embeddingModel,
        Integer embeddingDim,
        String embeddingHash,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ChatKnowledgeEmbeddingSummary from(ChatKnowledgeEmbeddingEntity entity) {
        return new ChatKnowledgeEmbeddingSummary(
                entity.getId(),
                entity.getGroupId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getEmbeddingModel(),
                entity.getEmbeddingDim(),
                entity.getEmbeddingHash(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
