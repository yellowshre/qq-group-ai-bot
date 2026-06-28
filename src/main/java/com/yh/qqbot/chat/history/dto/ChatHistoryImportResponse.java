package com.yh.qqbot.chat.history.dto;

public record ChatHistoryImportResponse(
        Long batchId,
        long rawMessages,
        long cleanMessages,
        long mentions,
        long replies,
        long sessions,
        long members,
        String status,
        boolean duplicateImport
) {
}
