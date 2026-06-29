package com.yh.qqbot.chat.history.dto;

import java.util.List;

public record KnowledgeSearchResponse(
        String query,
        List<KnowledgeSearchResult> results
) {
}
