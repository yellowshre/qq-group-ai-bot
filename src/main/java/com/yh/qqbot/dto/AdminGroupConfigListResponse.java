package com.yh.qqbot.dto;

import java.util.List;

public record AdminGroupConfigListResponse(
        List<String> allowedGroupIds,
        List<GroupConfigSnapshot> configuredGroups
) {
}
