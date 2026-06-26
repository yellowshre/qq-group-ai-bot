package com.yh.qqbot.service.meme;

import com.yh.qqbot.entity.SceneDictEntity;
import com.yh.qqbot.mapper.SceneDictMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SceneDictService {

    private final SceneDictMapper sceneDictMapper;

    public SceneDictService(SceneDictMapper sceneDictMapper) {
        this.sceneDictMapper = sceneDictMapper;
    }

    public List<SceneDictEntity> findAll() {
        return sceneDictMapper.selectList(null);
    }

    public Optional<SceneDictEntity> findBySceneCode(String sceneCode) {
        if (sceneCode == null || sceneCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sceneDictMapper.selectById(sceneCode.strip()));
    }
}
