package com.yh.qqbot.chat.history.service.cleaner;

import com.fasterxml.jackson.databind.JsonNode;
import com.yh.qqbot.chat.history.dto.ChatHistoryParsedMessage;
import com.yh.qqbot.chat.history.dto.ChatMessageCleanResult;
import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageMentionEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import com.yh.qqbot.chat.history.entity.ChatRawMessageEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageCleanService {

    private static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("type_1", "type_3");
    private static final Set<String> BLOCKED_ELEMENT_TYPES = Set.of(
            "image", "audio", "file", "video", "forward", "market_face"
    );
    private static final Pattern REPLY_HEAD_PATTERN = Pattern.compile("^\\[\\s*回复\\s+([^:：\\]]+)(?:[:：]\\s*([^\\]]*))?\\]\\s*");
    private static final Pattern LEADING_AT_PATTERN = Pattern.compile("^@[^\\s@\\[\\]:：,，]+\\s+");
    private static final Pattern ANY_AT_PATTERN = Pattern.compile("@([^\\s@\\[\\]:：,，]+)");
    private static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("^\\[[^\\]]{1,30}\\]$");

    public Optional<ChatMessageCleanResult> clean(ChatHistoryParsedMessage parsed, ChatRawMessageEntity raw) {
        if (!shouldKeep(parsed)) {
            return Optional.empty();
        }

        String text = normalizeText(parsed.rawText());
        if (text.isBlank() || isPlaceholder(text)) {
            return Optional.empty();
        }

        ChatMessageReplyEntity reply = extractReply(parsed, raw, text);
        String cleanText = removeReplyHead(text);
        cleanText = removeLeadingAt(cleanText).strip();
        if (cleanText.isBlank() || isPlaceholder(cleanText)) {
            return Optional.empty();
        }

        List<ChatMessageMentionEntity> mentions = extractMentions(parsed, raw, text);

        ChatCleanMessageEntity clean = new ChatCleanMessageEntity();
        clean.setRawMessageId(raw.getId());
        clean.setBatchId(raw.getBatchId());
        clean.setGroupId(raw.getGroupId());
        clean.setMessageId(raw.getMessageId());
        clean.setSeq(raw.getSeq());
        clean.setMessageTime(raw.getMessageTime());
        clean.setSenderUid(raw.getSenderUid());
        clean.setSenderUin(raw.getSenderUin());
        clean.setSenderName(raw.getSenderName());
        clean.setCleanText(cleanText);
        clean.setTextLength(cleanText.length());
        clean.setIsReply(reply != null);
        clean.setHasMention(!mentions.isEmpty());
        return Optional.of(new ChatMessageCleanResult(clean, mentions, reply));
    }

    private boolean shouldKeep(ChatHistoryParsedMessage message) {
        if (message.system() || message.recalled()) {
            return false;
        }
        if (message.rawText() == null || message.rawText().isBlank()) {
            return false;
        }
        if (!ALLOWED_MESSAGE_TYPES.contains(message.messageType())) {
            return false;
        }
        if (message.hasResource()) {
            return false;
        }
        return message.elementTypes().stream().noneMatch(BLOCKED_ELEMENT_TYPES::contains);
    }

    private ChatMessageReplyEntity extractReply(ChatHistoryParsedMessage parsed, ChatRawMessageEntity raw, String text) {
        ChatMessageReplyEntity structuredReply = extractStructuredReply(parsed, raw);
        if (structuredReply != null) {
            return structuredReply;
        }
        Matcher matcher = REPLY_HEAD_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        ChatMessageReplyEntity reply = new ChatMessageReplyEntity();
        reply.setRawMessageId(raw.getId());
        reply.setGroupId(raw.getGroupId());
        reply.setMessageId(raw.getMessageId());
        reply.setReplySenderName(blankToNull(matcher.group(1)));
        reply.setReplyContent(blankToNull(matcher.group(2)));
        return reply;
    }

    private ChatMessageReplyEntity extractStructuredReply(ChatHistoryParsedMessage parsed, ChatRawMessageEntity raw) {
        JsonNode elements = parsed.contentNode().path("elements");
        if (!elements.isArray()) {
            return null;
        }
        for (JsonNode element : elements) {
            String type = normalizeType(firstText(element, "type", "elementType", "name"));
            if (!"reply".equals(type)) {
                continue;
            }
            JsonNode data = element.has("data") ? element.path("data") : element;
            ChatMessageReplyEntity reply = new ChatMessageReplyEntity();
            reply.setRawMessageId(raw.getId());
            reply.setGroupId(raw.getGroupId());
            reply.setMessageId(raw.getMessageId());
            reply.setReplyMessageId(firstText(data, "id", "messageId", "replyMessageId"));
            reply.setReplySenderUin(firstText(data, "uin", "senderUin", "qq"));
            reply.setReplySenderName(firstText(data, "name", "senderName", "nickname"));
            reply.setReplyContent(firstText(data, "content", "text", "summary"));
            reply.setReplyTime(parseTime(firstText(data, "time", "replyTime")));
            return reply;
        }
        return null;
    }

    private List<ChatMessageMentionEntity> extractMentions(ChatHistoryParsedMessage parsed, ChatRawMessageEntity raw, String text) {
        Map<String, ChatMessageMentionEntity> mentions = new LinkedHashMap<>();
        addMentionsFromArray(mentions, parsed.contentNode().path("mentions"), raw);
        addMentionsFromElements(mentions, parsed.contentNode().path("elements"), raw);

        Matcher matcher = ANY_AT_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = blankToNull(matcher.group(1));
            if (name == null) {
                continue;
            }
            if (containsMentionName(mentions, name)) {
                continue;
            }
            ChatMessageMentionEntity mention = baseMention(raw);
            mention.setMentionedName(name);
            mentions.putIfAbsent("name:" + name, mention);
        }
        return List.copyOf(mentions.values());
    }

    private void addMentionsFromArray(
            Map<String, ChatMessageMentionEntity> mentions,
            JsonNode array,
            ChatRawMessageEntity raw) {
        if (!array.isArray()) {
            return;
        }
        for (JsonNode node : array) {
            ChatMessageMentionEntity mention = mentionFromNode(node, raw);
            mentions.putIfAbsent(mentionKey(mention), mention);
        }
    }

    private void addMentionsFromElements(
            Map<String, ChatMessageMentionEntity> mentions,
            JsonNode elements,
            ChatRawMessageEntity raw) {
        if (!elements.isArray()) {
            return;
        }
        for (JsonNode element : elements) {
            String type = normalizeType(firstText(element, "type", "elementType", "name"));
            if (!"at".equals(type) && !"mention".equals(type)) {
                continue;
            }
            JsonNode data = element.has("data") ? element.path("data") : element;
            ChatMessageMentionEntity mention = mentionFromNode(data, raw);
            mentions.putIfAbsent(mentionKey(mention), mention);
        }
    }

    private ChatMessageMentionEntity mentionFromNode(JsonNode node, ChatRawMessageEntity raw) {
        ChatMessageMentionEntity mention = baseMention(raw);
        mention.setMentionedUid(firstText(node, "uid", "uin", "qq", "id"));
        mention.setMentionedName(firstText(node, "name", "nickname", "displayName"));
        return mention;
    }

    private ChatMessageMentionEntity baseMention(ChatRawMessageEntity raw) {
        ChatMessageMentionEntity mention = new ChatMessageMentionEntity();
        mention.setRawMessageId(raw.getId());
        mention.setGroupId(raw.getGroupId());
        mention.setMessageId(raw.getMessageId());
        mention.setSenderUid(raw.getSenderUid());
        mention.setMentionType("AT");
        return mention;
    }

    private String mentionKey(ChatMessageMentionEntity mention) {
        if (mention.getMentionedUid() != null && !mention.getMentionedUid().isBlank()) {
            return "uid:" + mention.getMentionedUid();
        }
        if (mention.getMentionedName() != null && !mention.getMentionedName().isBlank()) {
            return "name:" + mention.getMentionedName();
        }
        return "unknown:" + mention.getMessageId() + ":" + mention.hashCode();
    }

    private boolean containsMentionName(Map<String, ChatMessageMentionEntity> mentions, String name) {
        return mentions.values().stream()
                .map(ChatMessageMentionEntity::getMentionedName)
                .anyMatch(existing -> existing != null && existing.equals(name));
    }

    private String removeReplyHead(String text) {
        return REPLY_HEAD_PATTERN.matcher(text).replaceFirst("");
    }

    private String removeLeadingAt(String text) {
        String result = text;
        while (true) {
            Matcher matcher = LEADING_AT_PATTERN.matcher(result);
            if (!matcher.find()) {
                return result;
            }
            result = matcher.replaceFirst("");
        }
    }

    private boolean isPlaceholder(String text) {
        String stripped = text.strip();
        return BRACKET_PLACEHOLDER_PATTERN.matcher(stripped).matches();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = blankToNull(value.asText(null));
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.strip().replace('-', '_').replace(' ', '_');
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1_$2");
        return normalized.toLowerCase();
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
