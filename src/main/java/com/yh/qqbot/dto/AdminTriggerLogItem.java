package com.yh.qqbot.dto;

import com.yh.qqbot.entity.TriggerLogEntity;
import java.time.LocalDateTime;

public record AdminTriggerLogItem(
        Long id,
        Long groupId,
        Long userId,
        String messageId,
        String originalMsg,
        String responseType,
        String responseText,
        Long memeId,
        String workflowType,
        Integer tokenUsed,
        String cost,
        Long durationMs,
        Boolean success,
        String errorMsg,
        LocalDateTime createdAt
) {

    public static AdminTriggerLogItem from(TriggerLogEntity entity) {
        return new AdminTriggerLogItem(
                entity.getId(),
                entity.getGroupId(),
                entity.getUserId(),
                entity.getMessageId(),
                limit(entity.getOriginalMsg()),
                entity.getResponseType(),
                limit(entity.getResponseText()),
                entity.getMemeId(),
                entity.getWorkflowType(),
                entity.getTokenUsed(),
                entity.getCost() == null ? null : entity.getCost().toPlainString(),
                entity.getDurationMs(),
                entity.getSuccess(),
                limit(entity.getErrorMsg()),
                entity.getCreatedAt());
    }

    private static String limit(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String stripped = value.strip();
        return stripped.length() <= 300 ? stripped : stripped.substring(0, 300) + "...";
    }
}
