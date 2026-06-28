package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatHistoryImportRequest(
        @NotBlank String groupId,
        @NotBlank String filePath
) {
}
