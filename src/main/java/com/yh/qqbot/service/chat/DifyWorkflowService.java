package com.yh.qqbot.service.chat;

import com.yh.qqbot.adapter.dify.DifyClient;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ActiveChatReplyResult;
import com.yh.qqbot.dto.ActiveChatRequest;
import com.yh.qqbot.dto.ChatPrompt;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.DifyActiveChatRequest;
import com.yh.qqbot.dto.DifyMemeSceneRequest;
import com.yh.qqbot.dto.DifyMemeSceneResponse;
import com.yh.qqbot.dto.DifyPassiveChatRequest;
import com.yh.qqbot.dto.DifyPassiveChatResponse;
import com.yh.qqbot.dto.PassiveChatReply;
import com.yh.qqbot.dto.SceneDecision;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void logDifyConfigStatus() {
        QqBotProperties.Dify dify = properties.getDify();
        log.info("Dify config status. enabled={}, baseUrlConfigured={}, memeSceneApiKeyConfigured={}, passiveChatApiKeyConfigured={}, activeChatApiKeyConfigured={}",
                dify.isEnabled(),
                hasText(dify.getBaseUrl()),
                hasText(dify.getMemeSceneApiKey()),
                hasText(dify.getPassiveChatApiKey()),
                hasText(dify.getActiveChatApiKey()));
    }

    public Optional<SceneDecision> classifyScene(String text, String userId) {
        return recognizeMemeScene(text, null, parseLong(userId));
    }

    public Optional<SceneDecision> recognizeMemeScene(String text, Long groupId, Long userId) {
        return recognizeMemeScene(text, groupId, userId, "");
    }

    public Optional<SceneDecision> recognizeMemeScene(String text, Long groupId, Long userId, String knowledgeContext) {
        QqBotProperties.Dify dify = properties.getDify();
        if (!dify.isEnabled() || !hasText(dify.getMemeSceneApiKey())) {
            return Optional.empty();
        }

        try {
            DifyMemeSceneRequest request = new DifyMemeSceneRequest(
                    text,
                    toInputString(groupId),
                    toInputString(userId),
                    knowledgeContext);
            String difyUser = userId == null ? null : String.valueOf(userId);
            return difyClient.runWorkflow(dify.getSceneWorkflowId(), request.toInputs(), difyUser, dify.getMemeSceneApiKey())
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
        return generatePassiveReply(text, groupId, userId, botName, persona, recentMessages, "");
    }

    public Optional<PassiveChatReply> generatePassiveReply(
            String text,
            Long groupId,
            Long userId,
            String botName,
            String persona,
            List<String> recentMessages,
            String knowledgeContext) {
        QqBotProperties.Dify dify = properties.getDify();
        if (!dify.isEnabled() || !hasText(dify.getPassiveChatApiKey())) {
            return Optional.empty();
        }
        if (!hasText(dify.getPassiveChatWorkflowId())) {
            log.warn("Skip Dify passive chat because workflow id is empty. difyEnabled={}, passiveChatApiKeyConfigured={}",
                    dify.isEnabled(), hasText(dify.getPassiveChatApiKey()));
            return Optional.empty();
        }

        try {
            DifyPassiveChatRequest request = new DifyPassiveChatRequest(
                    text,
                    toInputString(groupId),
                    toInputString(userId),
                    botName,
                    persona,
                    recentMessages,
                    knowledgeContext);
            String difyUser = userId == null ? null : String.valueOf(userId);
            return difyClient.runWorkflow(
                            dify.getPassiveChatWorkflowId(),
                            request.toInputs(),
                            difyUser,
                            dify.getPassiveChatApiKey())
                    .map(this::outputs)
                    .map(outputs -> new DifyPassiveChatResponse(
                            valueAsString(firstValue(outputs, "replyText", "reply_text")),
                            valueAsNullableDouble(firstValue(outputs, "confidence", "score"))
                    ))
                    .filter(response -> {
                        boolean valid = response.valid();
                        if (!valid) {
                            log.warn("Dify passive chat response invalid. replyTextPresent={}, confidencePresent={}",
                                    hasText(response.replyText()), response.confidence() != null);
                        }
                        return valid;
                    })
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
        List<String> recentMessages = new java.util.ArrayList<>();
        if (prompt.hotContext() != null) {
            recentMessages.addAll(prompt.hotContext());
        }
        if (prompt.coldSummaries() != null) {
            recentMessages.addAll(prompt.coldSummaries());
        }
        return generatePassiveReply(
                prompt.currentMessage(),
                parseLong(prompt.groupId()),
                parseLong(userId),
                botName(),
                prompt.persona(),
                recentMessages)
                .map(reply -> new ChatReply(reply.replyText(), "passive-chat"));
    }

    public boolean shouldActiveJoin(String groupId, String persona, String currentMessage, java.util.List<String> recentMessages) {
        return false;
    }

    public ActiveChatReplyResult generateActiveReply(ActiveChatRequest request) {
        QqBotProperties.Dify dify = properties.getDify();
        if (!dify.isEnabled()) {
            return ActiveChatReplyResult.rejected(ActiveChatReplyResult.DIFY_DISABLED);
        }
        if (!hasText(dify.getActiveChatApiKey())) {
            return ActiveChatReplyResult.rejected(ActiveChatReplyResult.API_KEY_MISSING);
        }

        try {
            DifyActiveChatRequest difyRequest = new DifyActiveChatRequest(
                    request == null ? "" : request.text(),
                    request == null ? "" : toInputString(request.groupId()),
                    request == null ? "" : toInputString(request.userId()),
                    request == null ? "" : request.botName(),
                    request == null ? "" : request.persona(),
                    request == null ? "" : request.recentMessages(),
                    request == null ? "" : request.activeReason(),
                    request == null ? "" : request.riskHint(),
                    request == null ? "" : request.knowledgeContext()
            );
            String difyUser = request == null ? null : toInputString(request.userId());
            Optional<Map<String, Object>> response = difyClient.runWorkflow(
                    dify.getActiveWorkflowId(),
                    difyRequest.toInputs(),
                    difyUser,
                    dify.getActiveChatApiKey());
            if (response.isEmpty()) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.DIFY_ERROR);
            }

            Map<String, Object> outputs = outputs(response.get());
            Boolean shouldReply = valueAsNullableBoolean(firstValue(outputs, "shouldReply", "should_reply"));
            if (shouldReply == null) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.INVALID_RESPONSE);
            }
            if (!shouldReply) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.SHOULD_REPLY_FALSE);
            }
            String replyText = valueAsString(firstValue(outputs, "replyText", "reply_text")).strip();
            if (replyText.isBlank()) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.EMPTY_REPLY);
            }
            Double confidence = valueAsNullableDouble(firstValue(outputs, "confidence", "score"));
            if (confidence == null) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.INVALID_RESPONSE, replyText, 0);
            }
            if (confidence < properties.getActiveChat().getMinConfidence()) {
                return ActiveChatReplyResult.rejected(ActiveChatReplyResult.LOW_CONFIDENCE, replyText, confidence);
            }
            return ActiveChatReplyResult.success(replyText, confidence);
        } catch (Exception ex) {
            log.warn("Dify active chat reply generation failed.", ex);
            return ActiveChatReplyResult.rejected(ActiveChatReplyResult.DIFY_ERROR);
        }
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

    private Boolean valueAsNullableBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).strip();
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("yes") || text.equals("1")) {
            return true;
        }
        if (text.equalsIgnoreCase("false") || text.equalsIgnoreCase("no") || text.equals("0")) {
            return false;
        }
        return null;
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

    private String botName() {
        String configured = properties.getIdentity().getDisplayName();
        if (hasText(configured)) {
            return configured.strip();
        }
        return properties.getNicknames().stream()
                .filter(this::hasText)
                .findFirst()
                .map(String::strip)
                .orElse("\u5c0f\u9ec4");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
