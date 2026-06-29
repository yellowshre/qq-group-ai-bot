package com.yh.qqbot.chat.history.dto;

import java.util.List;

public record KnowledgeRoutePreviewResponse(
        String groupId,
        String query,
        List<RouteKnowledgePreview> routes
) {
}
