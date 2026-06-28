package com.yh.qqbot.chat.history.service.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatKnowledgeCandidateGenerationServiceTest {

    @Test
    void phraseCandidateRuleRequiresRepeatedTextAndMultipleMembers() throws Exception {
        Object service = generationService();
        List<Object> messages = List.of(
                cleanMessage(1L, "m1", "经典", "u1"),
                cleanMessage(2L, "m2", "经典", "u2"),
                cleanMessage(3L, "m3", "经典", "u1"),
                cleanMessage(4L, "m4", "12345", "u3"),
                cleanMessage(5L, "m5", "！！！", "u4")
        );

        List<?> candidates = (List<?>) service.getClass()
                .getMethod("buildPhraseCandidates", Long.class, String.class, List.class)
                .invoke(service, 1L, "251288204", messages);

        assertThat(candidates).hasSize(1);
        Object candidate = candidates.get(0);
        assertThat(invoke(candidate, "getCandidateType")).isEqualTo("PHRASE");
        assertThat(invoke(candidate, "getContent")).isEqualTo("经典");
        assertThat(invoke(candidate, "getHitCount")).isEqualTo(3L);
        assertThat(invoke(candidate, "getMemberCount")).isEqualTo(2L);
    }

    @Test
    void memberScoreUsesConfiguredFormula() throws Exception {
        Object service = generationService();
        Object stat = cls("com.yh.qqbot.chat.history.entity.ChatMemberStatEntity")
                .getConstructor()
                .newInstance();
        set(stat, "messageCount", 10L);
        set(stat, "activeDays", 3);
        set(stat, "mentionCount", 2L);
        set(stat, "replyCount", 4L);
        set(stat, "repliedByCount", 5L);
        set(stat, "sessionCount", 6L);

        Object score = service.getClass()
                .getMethod("score", cls("com.yh.qqbot.chat.history.entity.ChatMemberStatEntity"))
                .invoke(service, stat);

        assertThat(score).isEqualTo(65L);
    }

    private Object generationService() throws Exception {
        return cls("com.yh.qqbot.chat.history.service.candidate.ChatKnowledgeCandidateGenerationService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper"))
                .newInstance(
                        mapper("com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatSessionMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper"));
    }

    private Object cleanMessage(Long id, String messageId, String text, String senderUid) throws Exception {
        Object message = cls("com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity")
                .getConstructor()
                .newInstance();
        set(message, "id", id);
        set(message, "batchId", 1L);
        set(message, "groupId", "251288204");
        set(message, "messageId", messageId);
        set(message, "messageTime", LocalDateTime.of(2026, 6, 28, 12, 0));
        set(message, "senderUid", senderUid);
        set(message, "cleanText", text);
        set(message, "isReply", false);
        return message;
    }

    private Object mapper(String className) throws Exception {
        Class<?> type = cls(className);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
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

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            if (returnType == List.class) {
                return List.of();
            }
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == void.class) {
            return null;
        }
        return 0;
    }
}
