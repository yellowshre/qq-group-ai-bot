package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeContextPreviewRequest(
        @NotBlank String groupId,
        @NotBlank String messageText,
        String senderUid,
        @NotBlank String routeType,
        Integer topK
) {
}
