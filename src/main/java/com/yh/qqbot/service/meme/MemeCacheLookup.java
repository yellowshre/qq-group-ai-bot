package com.yh.qqbot.service.meme;

import com.yh.qqbot.dto.MemeSemanticCacheEntry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface MemeCacheLookup {

    Optional<List<Long>> findMemeIdsByKeyword(String keyword);

    Optional<List<Long>> findMemeIdsBySceneCode(String sceneCode);

    default Optional<MemeSemanticCacheEntry> findSemanticScene(String text) {
        return Optional.empty();
    }

    default void cacheSemanticScene(String text, MemeSemanticCacheEntry entry, Duration ttl) {
    }
}
