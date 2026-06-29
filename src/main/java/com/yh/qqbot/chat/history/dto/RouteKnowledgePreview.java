package com.yh.qqbot.chat.history.dto;

import java.util.List;

public record RouteKnowledgePreview(
        String routeType,
        boolean routeKnowledgeEnabled,
        boolean knowledgeUsed,
        int itemCount,
        double maxScore,
        String knowledgeContext,
        List<KnowledgeContextItem> items,
        String silentReason
) {
}
