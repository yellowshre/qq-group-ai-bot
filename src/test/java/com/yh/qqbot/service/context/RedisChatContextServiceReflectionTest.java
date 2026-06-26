package com.yh.qqbot.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisChatContextServiceReflectionTest {

    private static final String NO_CONTEXT = "\u6682\u65e0\u4e0a\u4e0b\u6587";

    @Test
    void emptyContextReturnsNoContextText() throws Exception {
        TestRedis redis = new TestRedis();
        Object service = newService(redis.template(), 10);

        Object result = invoke(service, "getRecentMessages", new Class<?>[]{Long.class}, 10001L);

        assertThat(result).isEqualTo(NO_CONTEXT);
    }

    @Test
    void appendUserMessageCanBeLoaded() throws Exception {
        TestRedis redis = new TestRedis();
        Object service = newService(redis.template(), 10);

        invoke(service, "appendUserMessage", new Class<?>[]{Long.class, Long.class, String.class},
                10001L, 20001L, "hello");
        Object result = invoke(service, "getRecentMessages", new Class<?>[]{Long.class}, 10001L);

        assertThat((String) result).contains("20001").contains("hello");
    }

    @Test
    void appendBotReplyCanBeLoaded() throws Exception {
        TestRedis redis = new TestRedis();
        Object service = newService(redis.template(), 10);

        invoke(service, "appendBotReply", new Class<?>[]{Long.class, String.class, String.class},
                10001L, "bot", "hi");
        Object result = invoke(service, "getRecentMessages", new Class<?>[]{Long.class}, 10001L);

        assertThat((String) result).contains("bot").contains("hi");
    }

    @Test
    void maxSizeKeepsOnlyRecentMessages() throws Exception {
        TestRedis redis = new TestRedis();
        Object service = newService(redis.template(), 3);

        for (int i = 1; i <= 5; i++) {
            invoke(service, "appendMessage", new Class<?>[]{Long.class, String.class, String.class},
                    10001L, "role", "msg" + i);
        }
        Object result = invoke(service, "getRecentMessages", new Class<?>[]{Long.class}, 10001L);

        assertThat((String) result)
                .doesNotContain("msg1")
                .doesNotContain("msg2")
                .contains("msg3")
                .contains("msg4")
                .contains("msg5");
    }

    @Test
    void redisExceptionDoesNotLeakToCaller() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForList()).thenThrow(new RuntimeException("redis down"));
        Object service = newService(redisTemplate, 10);

        Object result = invoke(service, "getRecentMessages", new Class<?>[]{Long.class}, 10001L);

        assertThat(result).isEqualTo(NO_CONTEXT);
        assertDoesNotThrow(() -> invoke(service, "appendUserMessage",
                new Class<?>[]{Long.class, Long.class, String.class}, 10001L, 20001L, "hello"));
    }

    private Object newService(StringRedisTemplate redisTemplate, int maxSize) throws Exception {
        Object properties = properties(maxSize);
        return cls("com.yh.qqbot.service.context.RedisChatContextService")
                .getConstructor(StringRedisTemplate.class, cls("com.yh.qqbot.mapper.ChatSummaryMapper"), cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(redisTemplate, null, properties);
    }

    private Object properties(int maxSize) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object chatContext = invoke(properties, "getChatContext");
        invoke(chatContext, "setKeyPrefix", new Class<?>[]{String.class}, "qqbot:chat:ctx:");
        invoke(chatContext, "setMaxSize", new Class<?>[]{int.class}, maxSize);
        invoke(chatContext, "setTtlMinutes", new Class<?>[]{int.class}, 120);
        invoke(chatContext, "setMaxMessageLength", new Class<?>[]{int.class}, 160);
        return properties;
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private static class TestRedis {
        private final Map<String, List<String>> store = new HashMap<>();
        private final StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        private final ListOperations<String, String> listOps = mock(ListOperations.class);

        TestRedis() {
            when(template.opsForList()).thenReturn(listOps);
            when(template.expire(anyString(), any(Duration.class))).thenReturn(true);
            when(listOps.range(anyString(), anyLong(), anyLong()))
                    .thenAnswer(invocation -> range(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2)));
            when(listOps.rightPush(anyString(), anyString()))
                    .thenAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        String value = invocation.getArgument(1);
                        List<String> items = store.computeIfAbsent(key, ignored -> new ArrayList<>());
                        items.add(value);
                        return (long) items.size();
                    });
            doAnswer(invocation -> {
                trim(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                return null;
            }).when(listOps).trim(anyString(), anyLong(), anyLong());
        }

        StringRedisTemplate template() {
            return template;
        }

        private List<String> range(String key, long start, long end) {
            List<String> items = store.getOrDefault(key, List.of());
            if (items.isEmpty()) {
                return List.of();
            }
            int from = normalizeIndex(start, items.size());
            int to = end == -1 ? items.size() - 1 : normalizeIndex(end, items.size());
            if (from > to || from >= items.size()) {
                return List.of();
            }
            return new ArrayList<>(items.subList(Math.max(0, from), Math.min(items.size(), to + 1)));
        }

        private void trim(String key, long start, long end) {
            List<String> items = store.get(key);
            if (items == null || items.isEmpty()) {
                return;
            }
            int from = normalizeIndex(start, items.size());
            int to = end == -1 ? items.size() - 1 : normalizeIndex(end, items.size());
            if (from > to || from >= items.size()) {
                store.put(key, new ArrayList<>());
                return;
            }
            store.put(key, new ArrayList<>(items.subList(Math.max(0, from), Math.min(items.size(), to + 1))));
        }

        private int normalizeIndex(long index, int size) {
            long normalized = index < 0 ? size + index : index;
            if (normalized < 0) {
                return 0;
            }
            if (normalized >= size) {
                return size - 1;
            }
            return (int) normalized;
        }
    }
}
