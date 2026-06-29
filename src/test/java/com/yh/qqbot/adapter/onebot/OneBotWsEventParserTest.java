package com.yh.qqbot.adapter.onebot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OneBotWsEventParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesArrayTextGroupMessage() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": -1588384891,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "text", "data": {"text": "ws测试"}}
                  ],
                  "raw_message": "ws测试"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "groupId")).isEqualTo("736566774");
        assertThat(invoke(message, "userId")).isEqualTo("885391366");
        assertThat(invoke(message, "messageId")).isEqualTo("-1588384891");
        assertThat(invoke(message, "effectiveText")).isEqualTo("ws测试");
        assertThat(invoke(message, "atBot")).isEqualTo(false);
    }

    @Test
    void parsesRealSnowLumaGroupMessageSample() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": -1588384892,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "text", "data": {"text": "能收到么"}}
                  ],
                  "raw_message": "能收到么"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "groupId")).isEqualTo("736566774");
        assertThat(invoke(message, "userId")).isEqualTo("885391366");
        assertThat(invoke(message, "effectiveText")).isEqualTo("能收到么");
        assertThat(invoke(message, "triggersPassiveChat")).isEqualTo(false);
    }

    @Test
    void parsesAtBotFromArraySegmentsAndTextFallback() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": 123,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "at", "data": {"qq": "1771183256"}},
                    {"type": "text", "data": {"text": " 你好"}}
                  ],
                  "raw_message": ""
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "atBot")).isEqualTo(true);
        assertThat(invoke(message, "triggersPassiveChat")).isEqualTo(true);
        assertThat(invoke(message, "effectiveText")).isEqualTo("@小黄 你好");
        assertThat(invoke(message, "rawMessage")).isEqualTo("@1771183256 你好");
    }

    @Test
    void usesArrayTextAsPlainTextWhenRawMessageContainsCqCode() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": 124,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "at", "data": {"qq": "1771183256"}},
                    {"type": "text", "data": {"text": " 你好"}}
                  ],
                  "raw_message": "[CQ:at,qq=1771183256] 你好"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "atBot")).isEqualTo(true);
        assertThat(invoke(message, "effectiveText")).isEqualTo("@小黄 你好");
        assertThat(invoke(message, "rawMessage")).isEqualTo("[CQ:at,qq=1771183256] 你好");
    }

    @Test
    void preservesAtTriggerTraceInRoutedText() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": 126,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "at", "data": {"qq": "1771183256"}},
                    {"type": "text", "data": {"text": " 介绍一下你自己"}}
                  ],
                  "raw_message": "[CQ:at,qq=1771183256] 介绍一下你自己"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "atBot")).isEqualTo(true);
        assertThat(invoke(message, "effectiveText")).isEqualTo("@小黄 介绍一下你自己");
    }

    @Test
    void parsesNicknameMentionFromTextSegments() {
        Object parser = parser();

        Optional<?> result = parse(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": 125,
                  "group_id": 736566774,
                  "user_id": 885391366,
                  "message": [
                    {"type": "text", "data": {"text": "小黄 你在吗"}}
                  ],
                  "raw_message": "小黄 你在吗"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "atBot")).isEqualTo(false);
        assertThat(invoke(message, "triggersPassiveChat")).isEqualTo(true);
        assertThat(invoke(message, "mentionedBotNickname")).isEqualTo(true);
    }

    @Test
    void parsesPrivateTextMessage() {
        Object parser = parser();

        Optional<?> result = parsePrivate(parser, """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "private",
                  "message_id": 200,
                  "user_id": 885391366,
                  "message": [
                    {"type": "text", "data": {"text": "#群 736566774 状态"}}
                  ],
                  "raw_message": "#群 736566774 状态"
                }
                """);

        assertThat(result).isPresent();
        Object message = result.get();
        assertThat(invoke(message, "userId")).isEqualTo("885391366");
        assertThat(invoke(message, "messageId")).isEqualTo("200");
        assertThat(invoke(message, "effectiveText")).isEqualTo("#群 736566774 状态");
    }

    @Test
    void ignoresGroupOutsideWhitelist() {
        Object parser = parser();

        Optional<?> result = parse(parser, baseEvent("10000", "885391366"));

        assertThat(result).isEmpty();
    }

    @Test
    void ignoresSelfMessage() {
        Object parser = parser();

        Optional<?> result = parse(parser, baseEvent("736566774", "1771183256"));

        assertThat(result).isEmpty();
    }

    @Test
    void ignoresActionResponseOrHeartbeat() {
        Object parser = parser();

        assertThat(ignoredReason(parser, """
                {"status": "ok", "retcode": 0, "echo": "test-send-001"}
                """)).isEqualTo("ACTION_RESPONSE");
        assertThat(ignoredReason(parser, """
                {"post_type": "meta_event", "meta_event_type": "heartbeat"}
                """)).isEqualTo("HEARTBEAT_EVENT");
        assertThat(ignoredReason(parser, """
                {"post_type": "meta_event", "meta_event_type": "lifecycle"}
                """)).isEqualTo("LIFECYCLE_EVENT");
    }

    private String baseEvent(String groupId, String userId) {
        return """
                {
                  "time": 1782555054,
                  "self_id": 1771183256,
                  "post_type": "message",
                  "message_type": "group",
                  "message_id": 1,
                  "group_id": %s,
                  "user_id": %s,
                  "message": [
                    {"type": "text", "data": {"text": "hello"}}
                  ],
                  "raw_message": "hello"
                }
                """.formatted(groupId, userId);
    }

    private Object parser() {
        try {
            Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
            Object onebot = invoke(properties, "getOnebot");
            onebot.getClass().getMethod("setSelfId", String.class).invoke(onebot, "1771183256");
            onebot.getClass().getMethod("setAllowedGroupIds", List.class).invoke(onebot, List.of("736566774"));
            return cls("com.yh.qqbot.adapter.onebot.OneBotWsEventParser")
                    .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"), ObjectMapper.class)
                    .newInstance(properties, objectMapper);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Optional<?> parse(Object parser, String payload) {
        try {
            return (Optional<?>) parser.getClass().getMethod("parse", String.class).invoke(parser, payload);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Optional<?> parsePrivate(Object parser, String payload) {
        try {
            return (Optional<?>) parser.getClass().getMethod("parsePrivate", String.class).invoke(parser, payload);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String ignoredReason(Object parser, String payload) {
        try {
            Object result = parser.getClass().getMethod("parseDetailed", String.class).invoke(parser, payload);
            return (String) invoke(result, "ignoredReason");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
