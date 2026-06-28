package com.yh.qqbot.chat.history.service.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatSessionSplitServiceTest {

    @Test
    void splitsWhenGapIsGreaterThanFifteenMinutes() throws Exception {
        List<?> sessions = split(List.of(
                message(1L, LocalDateTime.of(2026, 6, 28, 12, 0)),
                message(2L, LocalDateTime.of(2026, 6, 28, 12, 10)),
                message(3L, LocalDateTime.of(2026, 6, 28, 12, 26))
        ));

        assertThat(sessions).hasSize(2);
        assertThat((List<?>) sessions.get(0)).hasSize(2);
        assertThat((List<?>) sessions.get(1)).hasSize(1);
    }

    @Test
    void keepsExactlyFifteenMinuteGapInSameSession() throws Exception {
        List<?> sessions = split(List.of(
                message(1L, LocalDateTime.of(2026, 6, 28, 12, 0)),
                message(2L, LocalDateTime.of(2026, 6, 28, 12, 15))
        ));

        assertThat(sessions).hasSize(1);
    }

    private List<?> split(List<Object> messages) throws Exception {
        Object service = cls("com.yh.qqbot.chat.history.service.session.ChatSessionSplitService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper"))
                .newInstance(null, null);
        return (List<?>) service.getClass().getMethod("split", List.class).invoke(service, messages);
    }

    private Object message(Long seq, LocalDateTime time) throws Exception {
        Object message = cls("com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity")
                .getConstructor()
                .newInstance();
        set(message, "id", seq);
        set(message, "seq", seq);
        set(message, "messageTime", time);
        set(message, "senderUid", "u" + seq);
        return message;
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

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
