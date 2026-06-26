package com.yh.qqbot.dto;

public record MemeMatchResult(
        Long memeId,
        String sceneCode,
        String filePath,
        String matchType,
        Double confidence,
        String missReason) {

    public static final String MATCH_KEYWORD = "MEME_KEYWORD";
    public static final String MATCH_SCENE = "MEME_SCENE";
    public static final String MATCH_SEMANTIC_CACHE = "MEME_SEMANTIC_CACHE";
    public static final String MATCH_DIFY_SCENE = "MEME_DIFY_SCENE";

    public static MemeMatchResult empty() {
        return empty("meme not matched");
    }

    public static MemeMatchResult empty(String missReason) {
        return new MemeMatchResult(null, null, null, null, null, missReason);
    }

    public static MemeMatchResult empty(String matchType, String sceneCode, Double confidence, String missReason) {
        return new MemeMatchResult(null, sceneCode, null, matchType, confidence, missReason);
    }

    public boolean matched() {
        return memeId != null && filePath != null && !filePath.isBlank();
    }
}
