package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record KnowledgeSearchRequest(
        @NotBlank String groupId,
        @NotBlank String query,
        Integer topK,
        List<String> targetTypes
) {
}
