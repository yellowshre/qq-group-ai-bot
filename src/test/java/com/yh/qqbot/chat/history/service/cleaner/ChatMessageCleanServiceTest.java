package com.yh.qqbot.chat.history.service.cleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatMessageCleanServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void keepsPlainTextAndExtractsMention() throws Exception {
        JsonNode content = objectMapper.readTree("""
                {
                  "text": "@alice hello",
                  "mentions": [
                    {"uid": "u-alice", "name": "alice"}
                  ],
                  "elements": [],
                  "resources": []
                }
                """);

        Optional<?> result = clean(parsed("m1", "@alice hello", "type_1", false, false, false, List.of(), content), raw("m1"));

        assertThat(result).isPresent();
        Object cleanMessage = invoke(result.get(), "cleanMessage");
        List<?> mentions = (List<?>) invoke(result.get(), "mentions");
        assertThat(invoke(cleanMessage, "getCleanText")).isEqualTo("hello");
        assertThat(mentions).hasSize(1);
        assertThat(invoke(mentions.get(0), "getMentionedUid")).isEqualTo("u-alice");
    }

    @Test
    void removesReplyHeadAndLeadingAt() throws Exception {
        JsonNode content = objectMapper.readTree("""
                {
                  "text": "[回复 alice: old text]\\n@alice new text",
                  "elements": [],
                  "resources": []
                }
                """);

        Optional<?> result = clean(parsed("m2", "[回复 alice: old text]\n@alice new text", "type_3", false, false, false, List.of(), content), raw("m2"));

        assertThat(result).isPresent();
        Object cleanMessage = invoke(result.get(), "cleanMessage");
        Object reply = invoke(result.get(), "reply");
        assertThat(invoke(cleanMessage, "getCleanText")).isEqualTo("new text");
        assertThat(reply).isNotNull();
        assertThat(invoke(reply, "getReplySenderName")).isEqualTo("alice");
    }

    @Test
    void filtersImageElementAndResources() throws Exception {
        JsonNode content = objectMapper.readTree("""
                {
                  "text": "image placeholder",
                  "elements": [{"type": "image"}],
                  "resources": []
                }
                """);

        Optional<?> result = clean(parsed("m3", "image placeholder", "type_1", false, false, false, List.of("image"), content), raw("m3"));
        assertThat(result).isEmpty();

        Optional<?> resourceResult = clean(parsed("m4", "file placeholder", "type_1", false, false, true, List.of(), content), raw("m4"));
        assertThat(resourceResult).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Optional<?> clean(Object parsed, Object raw) throws Exception {
        Object service = cls("com.yh.qqbot.chat.history.service.cleaner.ChatMessageCleanService")
                .getConstructor()
                .newInstance();
        Method method = service.getClass().getMethod("clean",
                cls("com.yh.qqbot.chat.history.dto.ChatHistoryParsedMessage"),
                cls("com.yh.qqbot.chat.history.entity.ChatRawMessageEntity"));
        return (Optional<?>) method.invoke(service, parsed, raw);
    }

    private Object parsed(
            String id,
            String rawText,
            String messageType,
            boolean system,
            boolean recalled,
            boolean hasResource,
            List<String> elementTypes,
            JsonNode content) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.ChatHistoryParsedMessage")
                .getConstructor(String.class, Long.class, LocalDateTime.class, String.class, String.class,
                        String.class, String.class, String.class, String.class, boolean.class, boolean.class,
                        boolean.class, List.class, JsonNode.class, JsonNode.class)
                .newInstance(id, 1L, LocalDateTime.of(2026, 6, 28, 12, 0),
                        "sender-uid", "sender-uin", "sender", "card", messageType, rawText,
                        system, recalled, hasResource, elementTypes, content, content);
    }

    private Object raw(String id) throws Exception {
        Object raw = cls("com.yh.qqbot.chat.history.entity.ChatRawMessageEntity")
                .getConstructor()
                .newInstance();
        set(raw, "id", 100L);
        set(raw, "batchId", 1L);
        set(raw, "groupId", "251288204");
        set(raw, "messageId", id);
        set(raw, "seq", 1L);
        set(raw, "messageTime", LocalDateTime.of(2026, 6, 28, 12, 0));
        set(raw, "senderUid", "sender-uid");
        set(raw, "senderUin", "sender-uin");
        set(raw, "senderName", "sender");
        return raw;
    }

    private static void set(Object target, String property, Object value) throws Exception {
        String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(setter) && method.getParameterCount() == 1) {
                method.invoke(target, value);
                return;
            }
        }
        throw new NoSuchMethodException(setter);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
