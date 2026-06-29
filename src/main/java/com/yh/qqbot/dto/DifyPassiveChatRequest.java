package com.yh.qqbot.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DifyPassiveChatRequest(
        String text,
        String groupId,
        String userId,
        String botName,
        String persona,
        List<String> recentMessages,
        String knowledgeContext) {

    public DifyPassiveChatRequest(
            String text,
            String groupId,
            String userId,
            String botName,
            String persona,
            List<String> recentMessages) {
        this(text, groupId, userId, botName, persona, recentMessages, "");
    }

    public Map<String, Object> toInputs() {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text == null ? "" : text);
        if (groupId != null) {
            inputs.put("groupId", groupId);
        }
        if (userId != null) {
            inputs.put("userId", userId);
        }
        inputs.put("botName", botName == null ? "" : botName);
        inputs.put("persona", persona == null ? "" : persona);
        inputs.put("recentMessages", recentMessages == null ? "" : String.join("\n", recentMessages));
        inputs.put("knowledgeContext", knowledgeContext == null ? "" : knowledgeContext);
        return inputs;
    }
}
