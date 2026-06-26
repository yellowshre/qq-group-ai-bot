package com.yh.qqbot.service.meme;

import com.yh.qqbot.dto.MemeMatchResult;
import com.yh.qqbot.entity.MemeMaterialEntity;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemeMatchService {

    private static final Logger log = LoggerFactory.getLogger(MemeMatchService.class);

    private final MemeCacheLookup memeCacheLookup;
    private final MemeMaterialService memeMaterialService;

    public MemeMatchService(
            MemeCacheLookup memeCacheLookup,
            MemeMaterialService memeMaterialService) {
        this.memeCacheLookup = memeCacheLookup;
        this.memeMaterialService = memeMaterialService;
    }

    public MemeMatchResult match(String text, String userId) {
        String keyword = normalizeKeyword(text);
        if (keyword == null) {
            return MemeMatchResult.empty();
        }
        try {
            Optional<List<Long>> cachedIds = memeCacheLookup.findMemeIdsByKeyword(keyword);
            if (cachedIds.isEmpty()) {
                return MemeMatchResult.empty();
            }

            List<MemeMaterialEntity> materials = memeMaterialService.findEnabledByIds(cachedIds.get());
            return memeMaterialService.weightedRandom(materials)
                    .map(material -> toResult(material, "keyword"))
                    .orElseGet(MemeMatchResult::empty);
        } catch (Exception ex) {
            log.warn("Keyword meme match failed. keyword={}", keyword, ex);
            return MemeMatchResult.empty();
        }
    }

    public MemeMatchResult matchBySceneCode(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return MemeMatchResult.empty();
        }
        try {
            Optional<List<Long>> cachedIds = memeCacheLookup.findMemeIdsBySceneCode(sceneCode.strip());
            if (cachedIds.isEmpty()) {
                return MemeMatchResult.empty();
            }

            List<MemeMaterialEntity> materials = memeMaterialService.findEnabledByIds(cachedIds.get());
            return memeMaterialService.weightedRandom(materials)
                    .map(material -> toResult(material, "scene"))
                    .orElseGet(MemeMatchResult::empty);
        } catch (Exception ex) {
            log.warn("Scene meme match failed. sceneCode={}", sceneCode, ex);
            return MemeMatchResult.empty();
        }
    }

    private MemeMatchResult toResult(MemeMaterialEntity material, String matchType) {
        return new MemeMatchResult(material.getMemeId(), material.getSceneCode(), material.getFilePath(), matchType);
    }

    private String normalizeKeyword(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.strip();
    }
}
