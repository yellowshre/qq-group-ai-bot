package com.yh.qqbot.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record DifyMemeSceneRequest(String text, Long groupId, Long userId) {

    public Map<String, Object> toInputs() {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text == null ? "" : text);
        if (groupId != null) {
            inputs.put("groupId", groupId);
        }
        if (userId != null) {
            inputs.put("userId", userId);
        }
        return inputs;
    }
}
