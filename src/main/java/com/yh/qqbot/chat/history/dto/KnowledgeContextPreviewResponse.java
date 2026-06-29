package com.yh.qqbot.chat.history.dto;

import java.util.List;

public record KnowledgeContextPreviewResponse(
        String routeType,
        String query,
        boolean knowledgeUsed,
        String knowledgeContext,
        List<KnowledgeContextItem> items,
        String silentReason
) {
}
