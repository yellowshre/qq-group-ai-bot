package com.yh.qqbot.dto;

import java.time.Instant;

public record MemeSemanticCacheEntry(String sceneCode, double confidence, Instant createdAt) {

    public boolean valid() {
        return sceneCode != null && !sceneCode.isBlank();
    }
}
