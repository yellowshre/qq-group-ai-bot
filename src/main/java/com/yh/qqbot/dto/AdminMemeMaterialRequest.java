package com.yh.qqbot.dto;

public record AdminMemeMaterialRequest(
        String keywords,
        String sceneCode,
        String sceneDesc,
        Integer weight,
        Boolean enabled,
        String filePath
) {
}
