package com.yh.qqbot.adapter.dev;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.dto.AdminMemeMaterialRequest;
import com.yh.qqbot.dto.AdminSceneDictRequest;
import com.yh.qqbot.entity.MemeMaterialEntity;
import com.yh.qqbot.entity.SceneDictEntity;
import com.yh.qqbot.mapper.MemeMaterialMapper;
import com.yh.qqbot.mapper.SceneDictMapper;
import com.yh.qqbot.service.meme.MemeCacheService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/admin/memes")
public class AdminMemeController {

    private final SceneDictMapper sceneDictMapper;
    private final MemeMaterialMapper memeMaterialMapper;
    private final MemeCacheService memeCacheService;

    public AdminMemeController(
            SceneDictMapper sceneDictMapper,
            MemeMaterialMapper memeMaterialMapper,
            MemeCacheService memeCacheService) {
        this.sceneDictMapper = sceneDictMapper;
        this.memeMaterialMapper = memeMaterialMapper;
        this.memeCacheService = memeCacheService;
    }

    @GetMapping("/scenes")
    public List<SceneDictEntity> scenes() {
        return sceneDictMapper.selectList(new LambdaQueryWrapper<SceneDictEntity>()
                .orderByAsc(SceneDictEntity::getSceneCode));
    }

    @PutMapping("/scenes/{sceneCode}")
    public SceneDictEntity upsertScene(
            @PathVariable String sceneCode,
            @RequestBody AdminSceneDictRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        String normalizedSceneCode = normalizeSceneCode(sceneCode);
        SceneDictEntity entity = sceneDictMapper.selectById(normalizedSceneCode);
        if (entity == null) {
            entity = new SceneDictEntity();
            entity.setSceneCode(normalizedSceneCode);
        }
        entity.setSceneDesc(requiredText(request.sceneDesc(), "sceneDesc is required", 255));
        entity.setConfidenceThreshold(confidenceThreshold(request.confidenceThreshold()));
        if (sceneDictMapper.selectById(normalizedSceneCode) == null) {
            sceneDictMapper.insert(entity);
        } else {
            sceneDictMapper.updateById(entity);
        }
        memeCacheService.preheat();
        return entity;
    }

    @GetMapping("/materials")
    public List<MemeMaterialEntity> materials(
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<MemeMaterialEntity> wrapper = new LambdaQueryWrapper<MemeMaterialEntity>()
                .eq(hasText(sceneCode), MemeMaterialEntity::getSceneCode, sceneCode == null ? null : sceneCode.strip())
                .eq(enabled != null, MemeMaterialEntity::getEnabled, enabled)
                .like(hasText(keyword), MemeMaterialEntity::getKeywords, keyword == null ? null : keyword.strip())
                .orderByAsc(MemeMaterialEntity::getSceneCode)
                .orderByDesc(MemeMaterialEntity::getEnabled)
                .orderByAsc(MemeMaterialEntity::getMemeId);
        return memeMaterialMapper.selectList(wrapper);
    }

    @GetMapping("/materials/{memeId}")
    public MemeMaterialEntity material(@PathVariable Long memeId) {
        MemeMaterialEntity entity = memeMaterialMapper.selectById(memeId);
        if (entity == null) {
            throw new IllegalArgumentException("meme material not found");
        }
        return entity;
    }

    @PostMapping("/materials")
    public MemeMaterialEntity createMaterial(@RequestBody AdminMemeMaterialRequest request) {
        MemeMaterialEntity entity = new MemeMaterialEntity();
        applyMaterial(entity, request);
        memeMaterialMapper.insert(entity);
        memeCacheService.preheat();
        return entity;
    }

    @PutMapping("/materials/{memeId}")
    public MemeMaterialEntity updateMaterial(
            @PathVariable Long memeId,
            @RequestBody AdminMemeMaterialRequest request) {
        MemeMaterialEntity entity = memeMaterialMapper.selectById(memeId);
        if (entity == null) {
            throw new IllegalArgumentException("meme material not found");
        }
        applyMaterial(entity, request);
        memeMaterialMapper.updateById(entity);
        memeCacheService.preheat();
        return entity;
    }

    @PostMapping("/cache/preheat")
    public Map<String, Object> preheatCache() {
        memeCacheService.preheat();
        return Map.of("success", true);
    }

    private void applyMaterial(MemeMaterialEntity entity, AdminMemeMaterialRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        entity.setKeywords(textOrEmpty(request.keywords(), 1024));
        entity.setSceneCode(optionalSceneCode(request.sceneCode()));
        entity.setSceneDesc(optionalText(request.sceneDesc(), 255));
        entity.setWeight(weight(request.weight()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setFilePath(requiredText(request.filePath(), "filePath is required", 1024));
    }

    private String normalizeSceneCode(String value) {
        String sceneCode = requiredText(value, "sceneCode is required", 64);
        if (!sceneCode.matches("[a-z0-9_\\-]+")) {
            throw new IllegalArgumentException("sceneCode must use lowercase letters, numbers, '_' or '-'");
        }
        return sceneCode;
    }

    private String optionalSceneCode(String value) {
        if (!hasText(value)) {
            return null;
        }
        return normalizeSceneCode(value);
    }

    private BigDecimal confidenceThreshold(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7500);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceThreshold must be between 0 and 1");
        }
        return value;
    }

    private Integer weight(Integer value) {
        if (value == null) {
            return 1;
        }
        if (value < 0) {
            throw new IllegalArgumentException("weight must be greater than or equal to 0");
        }
        return value;
    }

    private String requiredText(String value, String message, int maxLength) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return limit(value.strip(), maxLength);
    }

    private String optionalText(String value, int maxLength) {
        return hasText(value) ? limit(value.strip(), maxLength) : null;
    }

    private String textOrEmpty(String value, int maxLength) {
        return hasText(value) ? limit(value.strip(), maxLength) : "";
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
