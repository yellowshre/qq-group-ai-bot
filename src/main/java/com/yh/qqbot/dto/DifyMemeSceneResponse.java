package com.yh.qqbot.dto;

public record DifyMemeSceneResponse(String sceneCode, Double confidence) {

    public boolean valid() {
        return sceneCode != null && !sceneCode.isBlank() && confidence != null;
    }

    public SceneDecision toSceneDecision() {
        return new SceneDecision(sceneCode == null ? "" : sceneCode.strip(), confidence == null ? 0 : confidence);
    }
}
