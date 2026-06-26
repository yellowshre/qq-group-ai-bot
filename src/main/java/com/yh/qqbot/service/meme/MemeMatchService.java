package com.yh.qqbot.service.meme;

import com.yh.qqbot.dto.MemeMatchResult;
import com.yh.qqbot.dto.MemeSemanticCacheEntry;
import com.yh.qqbot.dto.SceneDecision;
import com.yh.qqbot.entity.MemeMaterialEntity;
import com.yh.qqbot.entity.SceneDictEntity;
import com.yh.qqbot.service.chat.DifyWorkflowService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemeMatchService {

    private static final Logger log = LoggerFactory.getLogger(MemeMatchService.class);
    private static final Duration SEMANTIC_CACHE_TTL = Duration.ofHours(1);

    private final MemeCacheLookup memeCacheLookup;
    private final MemeMaterialService memeMaterialService;
    private final SceneDictService sceneDictService;
    private final DifyWorkflowService difyWorkflowService;

    public MemeMatchService(
            MemeCacheLookup memeCacheLookup,
            MemeMaterialService memeMaterialService,
            SceneDictService sceneDictService,
            DifyWorkflowService difyWorkflowService) {
        this.memeCacheLookup = memeCacheLookup;
        this.memeMaterialService = memeMaterialService;
        this.sceneDictService = sceneDictService;
        this.difyWorkflowService = difyWorkflowService;
    }

    public MemeMatchResult match(String text, String userId) {
        return match(text, null, userId);
    }

    public MemeMatchResult match(String text, String groupId, String userId) {
        String normalizedText = normalizeKeyword(text);
        if (normalizedText == null) {
            return MemeMatchResult.empty("empty text");
        }

        Optional<MemeMatchResult> keywordResult = matchKeyword(normalizedText);
        if (keywordResult.isPresent()) {
            return keywordResult.get();
        }

        Optional<MemeMatchResult> cachedSemanticResult = matchSemanticCache(normalizedText);
        if (cachedSemanticResult.isPresent()) {
            return cachedSemanticResult.get();
        }

        return matchDifyScene(normalizedText, parseLong(groupId), parseLong(userId));
    }

    public MemeMatchResult matchBySceneCode(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return MemeMatchResult.empty("empty sceneCode");
        }
        return pickBySceneCode(sceneCode.strip(), MemeMatchResult.MATCH_SCENE, null)
                .orElseGet(() -> MemeMatchResult.empty(
                        MemeMatchResult.MATCH_SCENE,
                        sceneCode.strip(),
                        null,
                        "scene has no enabled meme material"));
    }

    private Optional<MemeMatchResult> matchKeyword(String keyword) {
        try {
            Optional<List<Long>> cachedIds = memeCacheLookup.findMemeIdsByKeyword(keyword);
            if (cachedIds.isEmpty()) {
                return Optional.empty();
            }

            List<MemeMaterialEntity> materials = memeMaterialService.findEnabledByIds(cachedIds.get());
            MemeMatchResult result = memeMaterialService.weightedRandom(materials)
                    .map(material -> toResult(material, MemeMatchResult.MATCH_KEYWORD, null))
                    .orElseGet(() -> MemeMatchResult.empty(
                            MemeMatchResult.MATCH_KEYWORD,
                            null,
                            null,
                            "keyword matched but no enabled meme material"));
            return Optional.of(result);
        } catch (Exception ex) {
            log.warn("Keyword meme match failed. keyword={}", keyword, ex);
            return Optional.of(MemeMatchResult.empty("keyword match failed"));
        }
    }

    private Optional<MemeMatchResult> matchSemanticCache(String text) {
        try {
            Optional<MemeSemanticCacheEntry> cached = memeCacheLookup.findSemanticScene(text);
            if (cached.isEmpty()) {
                return Optional.empty();
            }
            MemeSemanticCacheEntry entry = cached.get();
            return Optional.of(resolveScene(
                    entry.sceneCode(),
                    entry.confidence(),
                    MemeMatchResult.MATCH_SEMANTIC_CACHE));
        } catch (Exception ex) {
            log.warn("Meme semantic cache match failed.", ex);
            return Optional.of(MemeMatchResult.empty("semantic cache match failed"));
        }
    }

    private MemeMatchResult matchDifyScene(String text, Long groupId, Long userId) {
        try {
            Optional<SceneDecision> decision = difyWorkflowService.recognizeMemeScene(text, groupId, userId);
            if (decision.isEmpty()) {
                return MemeMatchResult.empty("meme not matched");
            }

            SceneDecision sceneDecision = decision.get();
            memeCacheLookup.cacheSemanticScene(
                    text,
                    new MemeSemanticCacheEntry(sceneDecision.sceneCode(), sceneDecision.confidence(), Instant.now()),
                    SEMANTIC_CACHE_TTL);
            return resolveScene(
                    sceneDecision.sceneCode(),
                    sceneDecision.confidence(),
                    MemeMatchResult.MATCH_DIFY_SCENE);
        } catch (Exception ex) {
            log.warn("Dify meme scene match failed.", ex);
            return MemeMatchResult.empty("dify scene recognition failed");
        }
    }

    private MemeMatchResult resolveScene(String sceneCode, double confidence, String matchType) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return MemeMatchResult.empty(matchType, null, confidence, "sceneCode is empty");
        }

        String normalizedSceneCode = sceneCode.strip();
        Optional<SceneDictEntity> scene = sceneDictService.findBySceneCode(normalizedSceneCode);
        if (scene.isEmpty()) {
            return MemeMatchResult.empty(matchType, normalizedSceneCode, confidence, "sceneCode not configured");
        }

        double threshold = thresholdOf(scene.get());
        if (confidence < threshold) {
            return MemeMatchResult.empty(
                    matchType,
                    normalizedSceneCode,
                    confidence,
                    "confidence below threshold");
        }

        return pickBySceneCode(normalizedSceneCode, matchType, confidence)
                .orElseGet(() -> MemeMatchResult.empty(
                        matchType,
                        normalizedSceneCode,
                        confidence,
                        "scene has no enabled meme material"));
    }

    private Optional<MemeMatchResult> pickBySceneCode(String sceneCode, String matchType, Double confidence) {
        try {
            List<MemeMaterialEntity> materials = memeCacheLookup.findMemeIdsBySceneCode(sceneCode)
                    .map(memeMaterialService::findEnabledByIds)
                    .orElseGet(() -> memeMaterialService.findBySceneCode(sceneCode));
            return memeMaterialService.weightedRandom(materials)
                    .map(material -> toResult(material, matchType, confidence));
        } catch (Exception ex) {
            log.warn("Scene meme match failed. sceneCode={}", sceneCode, ex);
            return Optional.empty();
        }
    }

    private MemeMatchResult toResult(MemeMaterialEntity material, String matchType, Double confidence) {
        return new MemeMatchResult(
                material.getMemeId(),
                material.getSceneCode(),
                material.getFilePath(),
                matchType,
                confidence,
                null);
    }

    private double thresholdOf(SceneDictEntity scene) {
        BigDecimal threshold = scene.getConfidenceThreshold();
        return threshold == null ? 0 : threshold.doubleValue();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeKeyword(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.strip();
    }
}
