package com.yh.qqbot.dto;

import java.util.List;

public record DevFullHealthResponse(
        List<String> activeProfiles,
        DependencyStatus mysql,
        DependencyStatus redis,
        boolean difyEnabled,
        boolean memeCachePreheatEnabled,
        String messageSenderType,
        Long sceneDictCount,
        Long enabledMemeMaterialCount
) {

    public record DependencyStatus(boolean reachable, String detail) {
    }
}
