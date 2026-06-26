package com.yh.qqbot.dto;

public record SceneDecision(String sceneCode, double confidence) {

    public boolean valid() {
        return sceneCode != null && !sceneCode.isBlank();
    }
}
