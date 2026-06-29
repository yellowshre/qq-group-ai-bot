package com.yh.qqbot.chat.history.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ChatHistoryImportServiceTest {

    @Test
    void importSkipsDuplicateMessageIdsAndGeneratesFallbackIds() throws Exception {
        Path file = Path.of("data", "chat-export", "import_duplicate_message_id_test.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {
                  "chatInfo": {"name": "test-group"},
                  "metadata": {"exporter": "test", "version": "1"},
                  "messages": [
                    {
                      "id": "dup-1",
                      "seq": 1,
                      "timestamp": 1760000001,
                      "type": 1,
                      "sender": {"uid": "u1", "uin": "10001", "name": "tester"},
                      "content": {"text": "hello one", "elements": []}
                    },
                    {
                      "id": "dup-1",
                      "seq": 2,
                      "timestamp": 1760000002,
                      "type": 1,
                      "sender": {"uid": "u1", "uin": "10001", "name": "tester"},
                      "content": {"text": "hello duplicate", "elements": []}
                    },
                    {
                      "seq": 3,
                      "timestamp": 1760000003,
                      "type": 1,
                      "sender": {"uid": "u1", "uin": "10001", "name": "tester"},
                      "content": {"text": "hello fallback a", "elements": []}
                    },
                    {
                      "seq": 4,
                      "timestamp": 1760000004,
                      "type": 1,
                      "sender": {"uid": "u1", "uin": "10001", "name": "tester"},
                      "content": {"text": "hello fallback b", "elements": []}
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);
        try {
            CapturingMapperState state = new CapturingMapperState();
            Object service = service(state);

            Object response = invoke(service, "importFile", new Class<?>[]{String.class, String.class},
                    "251288204", "data/chat-export/import_duplicate_message_id_test.json");

            assertThat(invoke(response, "status")).isEqualTo("SUCCESS");
            assertThat(invoke(response, "rawMessages")).isEqualTo(3L);
            assertThat(invoke(response, "cleanMessages")).isEqualTo(3L);
            assertThat(state.rawMessages).hasSize(3);
            assertThat(messageId(state.rawMessages.get(0))).isEqualTo("dup-1");
            assertThat(messageId(state.rawMessages.get(1))).startsWith("fallback-");
            assertThat(messageId(state.rawMessages.get(2))).startsWith("fallback-");
            assertThat(messageId(state.rawMessages.get(1))).isNotEqualTo(messageId(state.rawMessages.get(2)));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private Object service(CapturingMapperState state) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Object sessionSplitService = cls("com.yh.qqbot.chat.history.service.session.ChatSessionSplitService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper"))
                .newInstance(
                        mapper("com.yh.qqbot.chat.history.mapper.ChatSessionMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper", state));
        Object memberStatService = cls("com.yh.qqbot.chat.history.service.stat.ChatMemberStatService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper"))
                .newInstance(
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper", state));
        return cls("com.yh.qqbot.chat.history.service.ChatHistoryImportService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatRawMessageMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMessageMentionMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper"),
                        cls("com.yh.qqbot.chat.history.service.importer.ChatHistoryJsonParser"),
                        cls("com.yh.qqbot.chat.history.service.importer.ChatHistoryPathValidator"),
                        cls("com.yh.qqbot.chat.history.service.cleaner.ChatMessageCleanService"),
                        cls("com.yh.qqbot.chat.history.service.session.ChatSessionSplitService"),
                        cls("com.yh.qqbot.chat.history.service.stat.ChatMemberStatService"),
                        ObjectMapper.class)
                .newInstance(
                        mapper("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatRawMessageMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMessageMentionMapper", state),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper", state),
                        cls("com.yh.qqbot.chat.history.service.importer.ChatHistoryJsonParser")
                                .getConstructor(ObjectMapper.class)
                                .newInstance(objectMapper),
                        cls("com.yh.qqbot.chat.history.service.importer.ChatHistoryPathValidator")
                                .getConstructor()
                                .newInstance(),
                        cls("com.yh.qqbot.chat.history.service.cleaner.ChatMessageCleanService")
                                .getConstructor()
                                .newInstance(),
                        sessionSplitService,
                        memberStatService,
                        objectMapper);
    }

    private Object mapper(String className, CapturingMapperState state) throws Exception {
        Class<?> type = cls(className);
        InvocationHandler handler = (proxy, method, args) -> handleMapperMethod(state, method, args);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private Object handleMapperMethod(CapturingMapperState state, Method method, Object[] args) throws Exception {
        String name = method.getName();
        if ("insert".equals(name)) {
            Object entity = args[0];
            assignId(entity, state.nextId.getAndIncrement());
            state.capture(entity);
            return 1;
        }
        if ("updateById".equals(name)) {
            return 1;
        }
        if ("selectOne".equals(name)) {
            return null;
        }
        if ("toString".equals(name)) {
            return "capturingMapper";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(name)) {
            return false;
        }
        return null;
    }

    private void assignId(Object entity, Long id) throws Exception {
        Method setter = entity.getClass().getMethod("setId", Long.class);
        setter.invoke(entity, id);
    }

    private String messageId(Object rawMessage) throws Exception {
        return (String) invoke(rawMessage, "getMessageId");
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private static final class CapturingMapperState {
        private final AtomicLong nextId = new AtomicLong(1);
        private final List<Object> rawMessages = new ArrayList<>();

        private void capture(Object entity) {
            if ("com.yh.qqbot.chat.history.entity.ChatRawMessageEntity".equals(entity.getClass().getName())) {
                rawMessages.add(entity);
            }
        }
    }
}
