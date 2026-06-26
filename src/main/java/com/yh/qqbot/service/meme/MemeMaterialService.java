package com.yh.qqbot.service.meme;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.entity.MemeMaterialEntity;
import com.yh.qqbot.mapper.MemeMaterialMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class MemeMaterialService {

    private final MemeMaterialMapper memeMaterialMapper;

    public MemeMaterialService(MemeMaterialMapper memeMaterialMapper) {
        this.memeMaterialMapper = memeMaterialMapper;
    }

    public List<MemeMaterialEntity> findEnabled() {
        return memeMaterialMapper.selectList(new LambdaQueryWrapper<MemeMaterialEntity>()
                .eq(MemeMaterialEntity::getEnabled, true));
    }

    public List<MemeMaterialEntity> findEnabledByIds(Collection<Long> memeIds) {
        if (memeIds == null || memeIds.isEmpty()) {
            return List.of();
        }
        return memeMaterialMapper.selectList(new LambdaQueryWrapper<MemeMaterialEntity>()
                .eq(MemeMaterialEntity::getEnabled, true)
                .in(MemeMaterialEntity::getMemeId, memeIds));
    }

    public List<MemeMaterialEntity> findByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalized = keyword.strip();
        return findEnabled().stream()
                .filter(material -> keywordsOf(material).contains(normalized))
                .toList();
    }

    public List<MemeMaterialEntity> findBySceneCode(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return List.of();
        }
        return memeMaterialMapper.selectList(new LambdaQueryWrapper<MemeMaterialEntity>()
                .eq(MemeMaterialEntity::getEnabled, true)
                .eq(MemeMaterialEntity::getSceneCode, sceneCode.strip()));
    }

    public Optional<MemeMaterialEntity> weightedRandom(List<MemeMaterialEntity> materials) {
        if (materials == null || materials.isEmpty()) {
            return Optional.empty();
        }

        List<MemeMaterialEntity> enabledMaterials = materials.stream()
                .filter(this::enabled)
                .toList();
        if (enabledMaterials.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = enabledMaterials.stream()
                .map(MemeMaterialEntity::getWeight)
                .filter(weight -> weight != null && weight > 0)
                .mapToInt(Integer::intValue)
                .sum();
        if (totalWeight <= 0) {
            return Optional.of(enabledMaterials.get(0));
        }

        int cursor = ThreadLocalRandom.current().nextInt(totalWeight);
        for (MemeMaterialEntity material : enabledMaterials) {
            int weight = material.getWeight() == null || material.getWeight() <= 0 ? 0 : material.getWeight();
            cursor -= weight;
            if (cursor < 0) {
                return Optional.of(material);
            }
        }
        return Optional.of(enabledMaterials.get(enabledMaterials.size() - 1));
    }

    public List<String> keywordsOf(MemeMaterialEntity material) {
        if (material == null || material.getKeywords() == null || material.getKeywords().isBlank()) {
            return List.of();
        }
        return Arrays.stream(material.getKeywords().split("[,，\\s]+"))
                .map(String::strip)
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .toList();
    }

    private boolean enabled(MemeMaterialEntity material) {
        return material != null && Boolean.TRUE.equals(material.getEnabled());
    }
}
