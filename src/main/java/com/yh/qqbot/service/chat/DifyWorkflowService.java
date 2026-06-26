package com.yh.qqbot.service.chat;

import com.yh.qqbot.adapter.dify.DifyClient;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ChatPrompt;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.SceneDecision;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DifyWorkflowService {

    private final DifyClient difyClient;
    private final QqBotProperties properties;

    public DifyWorkflowService(DifyClient difyClient, QqBotProperties properties) {
        this.difyClient = difyClient;
        this.properties = properties;
    }

    public Optional<SceneDecision> classifyScene(String text, String userId) {
        if (!properties.getDify().isEnabled()) {
            return Optional.empty();
        }

        Map<String, Object> inputs = Map.of("text", text == null ? "" : text);
        return difyClient.runWorkflow(properties.getDify().getSceneWorkflowId(), inputs, userId)
                .map(this::outputs)
                .map(outputs -> new SceneDecision(
                        valueAsString(outputs.get("sceneCode")),
                        valueAsDouble(outputs.get("confidence"))
                ))
                .filter(SceneDecision::valid);
    }

    public Optional<ChatReply> generateReply(ChatPrompt prompt, String userId) {
        if (!properties.getDify().isEnabled()) {
            return Optional.of(new ChatReply("收到：" + prompt.currentMessage(), "stub"));
        }

        Map<String, Object> inputs = Map.of(
                "groupId", prompt.groupId(),
                "triggerType", prompt.triggerType(),
                "persona", prompt.persona(),
                "hotContext", prompt.hotContext(),
                "coldSummaries", prompt.coldSummaries(),
                "currentMessage", prompt.currentMessage()
        );
        return difyClient.runWorkflow(properties.getDify().getChatWorkflowId(), inputs, userId)
                .map(this::outputs)
                .map(outputs -> new ChatReply(
                        valueAsString(outputs.get("replyText")),
                        valueAsString(outputs.get("sceneCode"))
                ))
                .filter(ChatReply::hasText);
    }

    public boolean shouldActiveJoin(String groupId, String persona, String currentMessage, java.util.List<String> recentMessages) {
        if (!properties.getDify().isEnabled()) {
            return false;
        }

        Map<String, Object> inputs = Map.of(
                "groupId", groupId,
                "persona", persona,
                "currentMessage", currentMessage == null ? "" : currentMessage,
                "recentMessages", recentMessages
        );
        return difyClient.runWorkflow(properties.getDify().getActiveWorkflowId(), inputs, groupId)
                .map(this::outputs)
                .map(outputs -> valueAsString(outputs.getOrDefault("answer", outputs.get("result"))))
                .map(String::strip)
                .map(answer -> answer.equals("是") || answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("true"))
                .orElse(false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> outputs(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object outputs = dataMap.get("outputs");
            if (outputs instanceof Map<?, ?> outputsMap) {
                return (Map<String, Object>) outputsMap;
            }
        }
        Object outputs = response.get("outputs");
        if (outputs instanceof Map<?, ?> outputsMap) {
            return (Map<String, Object>) outputsMap;
        }
        return Map.of();
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double valueAsDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
