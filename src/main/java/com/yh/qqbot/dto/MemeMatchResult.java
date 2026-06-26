package com.yh.qqbot.dto;

public record MemeMatchResult(Long memeId, String sceneCode, String filePath, String matchType) {

    public static MemeMatchResult empty() {
        return new MemeMatchResult(null, null, null, null);
    }

    public boolean matched() {
        return memeId != null && filePath != null && !filePath.isBlank();
    }
}
