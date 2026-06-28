package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualKnowledgeCandidateRequest(
        @NotNull Long batchId,
        @NotBlank String groupId,
        @NotBlank String candidateType,
        @NotBlank String title,
        @NotBlank String content,
        String evidenceText,
        String reviewer,
        String reviewComment
) {
}
