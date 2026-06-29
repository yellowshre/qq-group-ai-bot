package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record KnowledgeEmbeddingGenerateRequest(
        @NotBlank String groupId,
        List<String> targetTypes,
        Boolean regenerate
) {
}
