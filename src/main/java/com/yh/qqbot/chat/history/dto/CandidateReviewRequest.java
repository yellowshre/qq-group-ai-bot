package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;

public record CandidateReviewRequest(
        @NotBlank String status,
        String reviewer,
        String reviewComment
) {
}
