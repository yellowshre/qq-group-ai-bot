package com.yh.qqbot.dto;

import com.yh.qqbot.adapter.onebot.OneBotImagePathResolver.ImagePathInspection;
import com.yh.qqbot.entity.MemeMaterialEntity;

public record AdminMemeFileCheckItem(
        Long memeId,
        String sceneCode,
        Boolean enabled,
        String filePath,
        String resolvedPath,
        String oneBotFile,
        Boolean exists,
        boolean checkable,
        boolean directReference,
        String warning
) {
    public static AdminMemeFileCheckItem from(MemeMaterialEntity entity, ImagePathInspection inspection) {
        return new AdminMemeFileCheckItem(
                entity.getMemeId(),
                entity.getSceneCode(),
                entity.getEnabled(),
                entity.getFilePath(),
                inspection.resolvedPath(),
                inspection.oneBotFile(),
                inspection.exists(),
                inspection.checkable(),
                inspection.directReference(),
                inspection.warning());
    }
}
