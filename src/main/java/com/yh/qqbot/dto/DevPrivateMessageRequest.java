package com.yh.qqbot.dto;

import jakarta.validation.constraints.NotBlank;

public record DevPrivateMessageRequest(
        @NotBlank String userId,
        String messageId,
        @NotBlank String rawMessage
) {
}
