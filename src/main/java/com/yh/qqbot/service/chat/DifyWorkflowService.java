package com.yh.qqbot.service.chat;

import com.yh.qqbot.adapter.dify.DifyClient;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ChatPrompt;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.DifyMemeSceneRequest;
import com.yh.qqbot.dto.DifyMemeSceneResponse;
import com.yh.qqbot.dto.DifyPassiveChatRequest;
import com.yh.qqbot.dto.DifyPassiveChatResponse;
import com.yh.qqbot.dto.PassiveChatReply;
import com.yh.qqbot.dto.SceneDecision;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DifyWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowService.class);

    private final DifyClient difyClient;
    private final QqBotProperties properties;

    public DifyWorkflowService(DifyClient difyClient, QqBotProperties properties) {
        this.difyClient = difyClient;
        this.properties = properties;
    }

    public Optional<SceneDecision> classifyScene(String text, String userId) {
        return recognizeMemeScene(text, null, parseLong(userId));
    }

    public Optional<SceneDecision> recognizeMemeScene(String text, Long groupId, Long userId) {
        if (!properties.getDify().isEnabled()) {
            return Optional.empty();
        }

        try {
            DifyMemeSceneRequest request = new DifyMemeSceneRequest(text, toInputString(groupId), toInputString(userId));
            String difyUser = userId == null ? null : String.valueOf(userId);
            return difyClient.runWorkflow(properties.getDify().getSceneWorkflowId(), request.toInputs(), difyUser)
                    .map(this::outputs)
                    .map(outputs -> new DifyMemeSceneResponse(
                            valueAsString(firstValue(outputs, "sceneCode", "scene_code")),
                            valueAsNullableDouble(firstValue(outputs, "confidence", "score"))
                    ))
                    .filter(DifyMemeSceneResponse::valid)
                    .map(DifyMemeSceneResponse::toSceneDecision);
        } catch (Exception ex) {
            log.warn("Dify meme scene recognition failed.", ex);
            return Optional.empty();
        }
    }

    public Optional<PassiveChatReply> generatePassiveReply(
            String text,
            Long groupId,
            Long userId,
            String botName,
            String persona,
            List<String> recentMessages) {
        if (!properties.getDify().isEnabled()) {
            return Optional.empty();
        }

        try {
            DifyPassiveChatRequest request = new DifyPassiveChatRequest(
                    text,
                    toInputString(groupId),
                    toInputString(userId),
                    botName,
                    persona,
                    recentMessages);
            String difyUser = userId == null ? null : String.valueOf(userId);
            return difyClient.runWorkflow(properties.getDify().getPassiveChatWorkflowId(), request.toInputs(), difyUser)
                    .map(this::outputs)
                    .map(outputs -> new DifyPassiveChatResponse(
                            valueAsString(firstValue(outputs, "replyText", "reply_text")),
                            valueAsNullableDouble(firstValue(outputs, "confidence", "score"))
                    ))
                    .filter(DifyPassiveChatResponse::valid)
                    .map(DifyPassiveChatResponse::toPassiveChatReply)
                    .filter(PassiveChatReply::hasText);
        } catch (Exception ex) {
            log.warn("Dify passive chat reply generation failed.", ex);
            return Optional.empty();
        }
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

    private Double valueAsNullableDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object firstValue(Map<String, Object> values, String... names) {
        for (String name : names) {
            if (values.containsKey(name)) {
                return values.get(name);
            }
        }
        return null;
    }

    private String toInputString(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
