package com.yh.qqbot.service.meme;

import java.util.List;
import java.util.Optional;

public interface MemeCacheLookup {

    Optional<List<Long>> findMemeIdsByKeyword(String keyword);

    Optional<List<Long>> findMemeIdsBySceneCode(String sceneCode);
}
