package com.yh.qqbot.chat.history.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryExportData(
        String chatName,
        String exporterName,
        String exporterVersion,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<ChatHistoryParsedMessage> messages
) {
}
