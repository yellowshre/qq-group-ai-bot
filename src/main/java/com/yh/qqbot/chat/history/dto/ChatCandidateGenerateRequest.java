package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatCandidateGenerateRequest(
        @NotNull Long batchId,
        @NotBlank String groupId
) {
}
