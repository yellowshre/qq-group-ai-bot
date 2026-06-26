package com.yh.qqbot.dto;

import java.util.List;

public record ChatPrompt(
        String groupId,
        String triggerType,
        String persona,
        List<String> hotContext,
        List<String> coldSummaries,
        String currentMessage
) {
}
