package com.yh.qqbot.chat.history.dto;

import java.util.List;
import java.util.Map;

public record DifyContextSimulateResponse(
        String routeType,
        String workflow,
        String query,
        boolean knowledgeUsed,
        String knowledgeContext,
        List<KnowledgeContextItem> items,
        Map<String, Object> inputs
) {
}
