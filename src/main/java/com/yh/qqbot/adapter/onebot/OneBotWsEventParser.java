package com.yh.qqbot.adapter.onebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotGroupMessage;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OneBotWsEventParser {

    private final QqBotProperties properties;
    private final ObjectMapper objectMapper;

    public OneBotWsEventParser(QqBotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<BotGroupMessage> parse(String payload) {
        return parseDetailed(payload).message();
    }

    public ParseResult parseDetailed(String payload) {
        if (payload == null || payload.isBlank()) {
            return ParseResult.ignored("EMPTY_PAYLOAD", "", "", "", "", "");
        }
        try {
            return parseDetailed(objectMapper.readTree(payload));
        } catch (Exception ex) {
            return ParseResult.ignored("JSON_PARSE_FAILED", "", "", "", "", "");
        }
    }

    public Optional<BotGroupMessage> parse(JsonNode root) {
        return parseDetailed(root).message();
    }

    public ParseResult parseDetailed(JsonNode root) {
        if (root == null || !root.isObject()) {
            return ParseResult.ignored("JSON_NOT_OBJECT", "", "", "", "", "");
        }
        String postType = text(root, "post_type");
        String messageType = text(root, "message_type");
        String groupId = text(root, "group_id");
        String userId = text(root, "user_id");
        String rawMessage = text(root, "raw_message");

        if (isActionResponse(root)) {
            return ignored("ACTION_RESPONSE", root, rawMessage);
        }
        if ("meta_event".equals(postType) && "heartbeat".equals(text(root, "meta_event_type"))) {
            return ignored("HEARTBEAT_EVENT", root, rawMessage);
        }
        if ("meta_event".equals(postType) && "lifecycle".equals(text(root, "meta_event_type"))) {
            return ignored("LIFECYCLE_EVENT", root, rawMessage);
        }
        if (!"message".equals(postType)) {
            return ignored("POST_TYPE_NOT_MESSAGE", root, rawMessage);
        }
        if (!"group".equals(messageType)) {
            return ignored("MESSAGE_TYPE_NOT_GROUP", root, rawMessage);
        }

        if (!isAllowedGroup(groupId)) {
            return ignored("GROUP_NOT_ALLOWED", root, rawMessage);
        }

        String selfId = configuredSelfId(root);
        if (hasText(selfId) && selfId.equals(userId)) {
            return ignored("SELF_MESSAGE", root, rawMessage);
        }

        MessageParts parts = messageParts(root.path("message"), selfId);
        String plainText = routedText(parts, rawMessage);
        if (!hasText(plainText)) {
            return ignored("TEXT_EMPTY", root, rawMessage);
        }

        if (!hasText(rawMessage)) {
            rawMessage = parts.atBot() && hasText(selfId) ? "@" + selfId + " " + parts.text() : parts.text();
        }

        return ParseResult.routable(new BotGroupMessage(
                groupId,
                userId,
                text(root, "message_id"),
                rawMessage,
                plainText,
                parts.atBot(),
                mentionedBotNickname(plainText),
                receivedAt(root)
        ), postType, messageType, groupId, userId, rawMessage);
    }

    private boolean isAllowedGroup(String groupId) {
        if (!hasText(groupId)) {
            return false;
        }
        Set<String> allowed = new HashSet<>();
        for (String value : properties.getOnebot().getAllowedGroupIds()) {
            if (!hasText(value)) {
                continue;
            }
            for (String part : value.split(",")) {
                if (hasText(part)) {
                    allowed.add(part.strip());
                }
            }
        }
        return !allowed.isEmpty() && allowed.contains(groupId);
    }

    private MessageParts messageParts(JsonNode messageNode, String selfId) {
        if (!messageNode.isArray()) {
            return new MessageParts("", false);
        }
        StringBuilder text = new StringBuilder();
        boolean atBot = false;
        for (JsonNode segment : messageNode) {
            String type = text(segment, "type");
            JsonNode data = segment.path("data");
            if ("text".equals(type)) {
                text.append(text(data, "text"));
            } else if ("at".equals(type) && hasText(selfId) && selfId.equals(text(data, "qq"))) {
                atBot = true;
            }
        }
        return new MessageParts(text.toString().strip(), atBot);
    }

    private String routedText(MessageParts parts, String rawMessage) {
        String text = hasText(parts.text()) ? parts.text() : rawMessage;
        if (!hasText(text)) {
            return "";
        }
        if (parts.atBot()) {
            return "@" + botMentionLabel() + " " + text.strip();
        }
        return text;
    }

    private String botMentionLabel() {
        String displayName = properties.getIdentity().getDisplayName();
        if (hasText(displayName)) {
            return displayName.strip();
        }
        for (String nickname : properties.getNicknames()) {
            if (hasText(nickname)) {
                return nickname.strip();
            }
        }
        return "机器人";
    }

    private boolean isActionResponse(JsonNode root) {
        return root.has("echo") || (root.has("status") && root.has("retcode"));
    }

    private ParseResult ignored(String reason, JsonNode root, String rawMessage) {
        return ParseResult.ignored(
                reason,
                text(root, "post_type"),
                text(root, "message_type"),
                text(root, "group_id"),
                text(root, "user_id"),
                rawMessage
        );
    }

    private String configuredSelfId(JsonNode root) {
        String configured = properties.getOnebot().getSelfId();
        if (hasText(configured)) {
            return configured.strip();
        }
        return text(root, "self_id");
    }

    private boolean mentionedBotNickname(String text) {
        if (!hasText(text)) {
            return false;
        }
        return containsAny(text, properties.getPassiveChat().getTriggerWords())
                || containsAny(text, properties.getIdentity().getAliases())
                || containsAny(text, properties.getNicknames());
    }

    private boolean containsAny(String text, List<String> words) {
        if (words == null || words.isEmpty()) {
            return false;
        }
        for (String word : words) {
            if (hasText(word) && text.contains(word.strip())) {
                return true;
            }
        }
        return false;
    }

    private Instant receivedAt(JsonNode root) {
        JsonNode time = root.get("time");
        if (time != null && time.canConvertToLong()) {
            return Instant.ofEpochSecond(time.asLong());
        }
        return Instant.now();
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record MessageParts(String text, boolean atBot) {
    }

    public record ParseResult(
            Optional<BotGroupMessage> message,
            String ignoredReason,
            String postType,
            String messageType,
            String groupId,
            String userId,
            String rawMessage
    ) {

        private static ParseResult routable(
                BotGroupMessage message,
                String postType,
                String messageType,
                String groupId,
                String userId,
                String rawMessage) {
            return new ParseResult(Optional.of(message), "", postType, messageType, groupId, userId, rawMessage);
        }

        private static ParseResult ignored(
                String reason,
                String postType,
                String messageType,
                String groupId,
                String userId,
                String rawMessage) {
            return new ParseResult(Optional.empty(), reason, postType, messageType, groupId, userId, rawMessage);
        }
    }
}
