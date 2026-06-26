package com.yh.qqbot.dto;

public record MemeSelection(Long memeId, String sceneCode, String filePath) {

    public static MemeSelection empty() {
        return new MemeSelection(null, null, null);
    }

    public boolean matched() {
        return filePath != null && !filePath.isBlank();
    }
}
