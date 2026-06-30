package com.yh.qqbot.dto;

import com.yh.qqbot.entity.AdminOpLogEntity;
import java.time.LocalDateTime;

public record AdminOpLogItem(
        Long id,
        Long groupId,
        Long operatorUid,
        String operation,
        String detail,
        LocalDateTime createdAt
) {

    public static AdminOpLogItem from(AdminOpLogEntity entity) {
        return new AdminOpLogItem(
                entity.getId(),
                entity.getGroupId(),
                entity.getOperatorUid(),
                entity.getOperation(),
                limit(entity.getDetail()),
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
