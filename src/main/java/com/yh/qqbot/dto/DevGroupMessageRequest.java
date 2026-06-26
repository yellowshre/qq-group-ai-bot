package com.yh.qqbot.dto;

import jakarta.validation.constraints.NotBlank;

public record DevGroupMessageRequest(
        @NotBlank String groupId,
        @NotBlank String userId,
        String messageId,
        @NotBlank String rawMessage,
        Boolean atBot,
        Boolean botNicknameMatched
) {
}
