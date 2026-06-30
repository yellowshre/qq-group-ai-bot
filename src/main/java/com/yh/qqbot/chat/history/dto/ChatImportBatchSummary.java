package com.yh.qqbot.chat.history.dto;

import java.time.LocalDateTime;

public record ChatImportBatchSummary(
        Long batchId,
        String groupId,
        String sourceFile,
        String chatName,
        String exporterName,
        String exporterVersion,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long totalMessages,
        Long rawCount,
        Long cleanCount,
        Long mentionCount,
        Long replyCount,
        Long sessionCount,
        Long memberCount,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
