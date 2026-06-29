package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;

public record DifyContextSimulateRequest(
        @NotBlank String groupId,
        @NotBlank String messageText,
        String senderUid,
        @NotBlank String routeType,
        Integer topK,
        String botName,
        String persona,
        String recentMessages,
        String activeReason,
        String riskHint
) {
}
