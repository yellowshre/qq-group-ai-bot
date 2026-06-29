package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FormalKnowledgePublishRequest(
        @NotBlank String groupId,
        @NotEmpty List<Long> candidateIds,
        String operator,
        String comment
) {
}
