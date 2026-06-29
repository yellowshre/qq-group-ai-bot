package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeRoutePreviewRequest(
        @NotBlank String groupId,
        @NotBlank String messageText,
        String senderUid,
        Integer topK
) {
}
