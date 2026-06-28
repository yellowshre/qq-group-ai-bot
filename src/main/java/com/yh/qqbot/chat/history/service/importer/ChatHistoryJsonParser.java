package com.yh.qqbot.chat.history.service.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.chat.history.dto.ChatHistoryExportData;
import com.yh.qqbot.chat.history.dto.ChatHistoryParsedMessage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryJsonParser {

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private final ObjectMapper objectMapper;

    public ChatHistoryJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChatHistoryExportData parse(Path jsonFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonFile.toFile());
        JsonNode messagesNode = root.path("messages");
        if (!messagesNode.isArray()) {
            throw new IllegalArgumentException("messages must be an array");
        }

        List<ChatHistoryParsedMessage> messages = new ArrayList<>();
        for (JsonNode messageNode : messagesNode) {
            messages.add(parseMessage(messageNode));
        }

        LocalDateTime start = messages.stream()
                .map(ChatHistoryParsedMessage::messageTime)
                .filter(time -> time != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime end = messages.stream()
                .map(ChatHistoryParsedMessage::messageTime)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new ChatHistoryExportData(
                firstText(root.path("chatInfo"), "name", "chatName", "title"),
                firstText(root.path("metadata"), "exporter", "exporterName", "source"),
                firstText(root.path("metadata"), "version", "exporterVersion"),
                start,
                end,
                messages
        );
    }

    private ChatHistoryParsedMessage parseMessage(JsonNode node) {
        JsonNode sender = node.path("sender");
        JsonNode content = node.path("content");
        String type = normalizeType(node.path("type"));
        String rawText = firstText(content, "text", "plainText", "rawText");
        boolean hasResource = nonEmptyArray(content.path("resources"));
        List<String> elementTypes = elementTypes(content.path("elements"));
        return new ChatHistoryParsedMessage(
                firstText(node, "id", "messageId"),
                longValue(node.path("seq")),
                parseTime(node),
                firstText(sender, "uid"),
                firstText(sender, "uin"),
                firstText(sender, "name", "nickname", "remark"),
                firstText(sender, "groupCard"),
                type,
                rawText,
                booleanValue(node.path("system")),
                booleanValue(node.path("recalled")),
                hasResource,
                elementTypes,
                content,
                node
        );
    }

    private String normalizeType(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isNumber()) {
            return "type_" + node.asText();
        }
        return node.asText("");
    }

    private List<String> elementTypes(JsonNode elements) {
        if (!elements.isArray()) {
            return List.of();
        }
        Set<String> types = new LinkedHashSet<>();
        for (JsonNode element : elements) {
            String type = firstText(element, "type", "elementType", "name");
            if (type != null && !type.isBlank()) {
                types.add(normalizeElementType(type));
            }
        }
        return List.copyOf(types);
    }

    private String normalizeElementType(String type) {
        String normalized = type.strip().replace('-', '_').replace(' ', '_');
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1_$2");
        return normalized.toLowerCase();
    }

    private LocalDateTime parseTime(JsonNode node) {
        LocalDateTime timestampTime = parseTimestamp(node.path("timestamp"));
        if (timestampTime != null) {
            return timestampTime;
        }
        String time = firstText(node, "time", "datetime");
        if (time == null || time.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(time.strip(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next known exporter format.
            }
        }
        return null;
    }

    private LocalDateTime parseTimestamp(JsonNode timestamp) {
        if (timestamp == null || timestamp.isMissingNode() || timestamp.isNull()) {
            return null;
        }
        try {
            long value = timestamp.asLong();
            if (value <= 0) {
                return null;
            }
            Instant instant = value > 1_000_000_000_000L
                    ? Instant.ofEpochMilli(value)
                    : Instant.ofEpochSecond(value);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        try {
            String text = node.asText(null);
            return text == null || text.isBlank() ? null : Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean booleanValue(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull() && node.asBoolean(false);
    }

    private boolean nonEmptyArray(JsonNode node) {
        return node != null && node.isArray() && node.size() > 0;
    }
}
