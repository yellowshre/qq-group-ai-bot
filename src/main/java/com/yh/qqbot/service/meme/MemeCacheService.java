package com.yh.qqbot.service.meme;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.entity.MemeMaterialEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MemeCacheService implements ApplicationRunner, MemeCacheLookup {

    private static final Logger log = LoggerFactory.getLogger(MemeCacheService.class);
    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };

    private final MemeMaterialService memeMaterialService;
    private final StringRedisTemplate memeRedisTemplate;
    private final ObjectMapper objectMapper;
    private final QqBotProperties properties;

    public MemeCacheService(
            MemeMaterialService memeMaterialService,
            @Qualifier("memeStringRedisTemplate") StringRedisTemplate memeRedisTemplate,
            ObjectMapper objectMapper,
            QqBotProperties properties) {
        this.memeMaterialService = memeMaterialService;
        this.memeRedisTemplate = memeRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getMeme().isCachePreheatEnabled()) {
            log.info("Meme cache preheat disabled.");
            return;
        }
        preheat();
    }

    public void preheat() {
        try {
            List<MemeMaterialEntity> materials = memeMaterialService.findEnabled();
            Map<String, List<Long>> keywordIndex = new LinkedHashMap<>();
            Map<String, List<Long>> sceneIndex = new LinkedHashMap<>();

            for (MemeMaterialEntity material : materials) {
                Long memeId = material.getMemeId();
                if (memeId == null) {
                    continue;
                }
                for (String keyword : memeMaterialService.keywordsOf(material)) {
                    keywordIndex.computeIfAbsent(keyword, ignored -> new ArrayList<>()).add(memeId);
                }
                if (material.getSceneCode() != null && !material.getSceneCode().isBlank()) {
                    sceneIndex.computeIfAbsent(material.getSceneCode().strip(), ignored -> new ArrayList<>()).add(memeId);
                }
            }

            writeIndex(keywordIndex, true);
            writeIndex(sceneIndex, false);
            log.info("Meme cache preheat completed. success=true, keywords={}, scenes={}",
                    keywordIndex.size(), sceneIndex.size());
        } catch (Exception ex) {
            log.error("Meme cache preheat failed. success=false. Check MySQL and Redis availability.", ex);
        }
    }

    @Override
    public Optional<List<Long>> findMemeIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        return readIdList(RedisKeys.memeKeyword(keyword.strip()));
    }

    @Override
    public Optional<List<Long>> findMemeIdsBySceneCode(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return Optional.empty();
        }
        return readIdList(RedisKeys.memeScene(sceneCode.strip()));
    }

    private void writeIndex(Map<String, List<Long>> index, boolean keywordIndex) throws Exception {
        for (Map.Entry<String, List<Long>> entry : index.entrySet()) {
            String key = keywordIndex ? RedisKeys.memeKeyword(entry.getKey()) : RedisKeys.memeScene(entry.getKey());
            memeRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(entry.getValue()));
        }
    }

    private Optional<List<Long>> readIdList(String key) {
        try {
            String json = memeRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<Long> memeIds = objectMapper.readValue(json, LONG_LIST_TYPE);
            return memeIds == null || memeIds.isEmpty() ? Optional.empty() : Optional.of(memeIds);
        } catch (Exception ex) {
            log.debug("Read meme cache failed. key={}", key, ex);
            return Optional.empty();
        }
    }
}
