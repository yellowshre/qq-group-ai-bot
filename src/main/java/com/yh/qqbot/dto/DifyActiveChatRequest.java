package com.yh.qqbot.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record DifyActiveChatRequest(
        String text,
        String groupId,
        String userId,
        String botName,
        String persona,
        String recentMessages,
        String activeReason,
        String riskHint) {

    public Map<String, Object> toInputs() {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text == null ? "" : text);
        inputs.put("groupId", groupId == null ? "" : groupId);
        inputs.put("userId", userId == null ? "" : userId);
        inputs.put("botName", botName == null ? "" : botName);
        inputs.put("persona", persona == null ? "" : persona);
        inputs.put("recentMessages", recentMessages == null ? "" : recentMessages);
        inputs.put("activeReason", activeReason == null ? "" : activeReason);
        inputs.put("riskHint", riskHint == null ? "" : riskHint);
        return inputs;
    }
}
