package com.yh.qqbot.chat.history.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class KnowledgeContextServiceTest {

    @Test
    void memeContextUsesOnlyMemeSafeKnowledgeTypes() throws Exception {
        State state = new State();
        state.searchResults = List.of(
                searchResult("MEMBER_PROFILE", 10L, 0.95d),
                searchResult("GROUP_KNOWLEDGE", 1L, 0.9d),
                searchResult("GROUP_KNOWLEDGE", 2L, 0.85d),
                searchResult("GROUP_KNOWLEDGE", 3L, 0.8d),
                searchResult("GROUP_KNOWLEDGE", 4L, 0.99d));
        state.knowledge.put(1L, knowledge(1L, "PHRASE", "phrase", "phrase content", true, "ACTIVE"));
        state.knowledge.put(2L, knowledge(2L, "TOPIC", "topic", "topic content", true, "ACTIVE"));
        state.knowledge.put(3L, knowledge(3L, "MEME_SCENE", "scene", "scene content", true, "ACTIVE"));
        state.knowledge.put(4L, knowledge(4L, "REPLY_PATTERN", "reply", "reply content", true, "ACTIVE"));
        state.profiles.put(10L, profile(10L, true, "ACTIVE"));

        Object response = preview(service(state), "MEME");
        List<?> items = (List<?>) invoke(response, "items");

        assertThat(items).hasSize(2);
        assertThat(invoke(items.get(0), "type")).isEqualTo("PHRASE");
        assertThat(invoke(items.get(1), "type")).isEqualTo("MEME_SCENE");
        assertThat(invoke(response, "knowledgeUsed")).isEqualTo(true);
    }

    @Test
    void passiveContextAllowsLimitedMemberProfile() throws Exception {
        State state = new State();
        state.searchResults = List.of(
                searchResult("MEMBER_PROFILE", 10L, 0.95d),
                searchResult("MEMBER_PROFILE", 11L, 0.94d),
                searchResult("GROUP_KNOWLEDGE", 1L, 0.9d),
                searchResult("GROUP_KNOWLEDGE", 2L, 0.85d));
        state.knowledge.put(1L, knowledge(1L, "PHRASE", "phrase", "phrase content", true, "ACTIVE"));
        state.knowledge.put(2L, knowledge(2L, "TOPIC", "topic", "topic content", true, "ACTIVE"));
        state.profiles.put(10L, profile(10L, true, "ACTIVE"));
        state.profiles.put(11L, profile(11L, true, "ACTIVE"));

        Object response = preview(service(state), "PASSIVE_CHAT");
        List<?> items = (List<?>) invoke(response, "items");

        assertThat(items).hasSize(3);
        assertThat(items.stream().map(item -> uncheckedInvoke(item, "type")))
                .containsExactly("MEMBER_PROFILE", "PHRASE", "TOPIC");
    }

    @Test
    void activeContextExcludesMemberProfilesMemeSceneAndLowScore() throws Exception {
        State state = new State();
        state.searchResults = List.of(
                searchResult("MEMBER_PROFILE", 10L, 0.95d),
                searchResult("GROUP_KNOWLEDGE", 1L, 0.9d),
                searchResult("GROUP_KNOWLEDGE", 2L, 0.85d),
                searchResult("GROUP_KNOWLEDGE", 3L, 0.8d),
                searchResult("GROUP_KNOWLEDGE", 4L, 0.2d),
                searchResult("GROUP_KNOWLEDGE", 5L, 0.99d));
        state.knowledge.put(1L, knowledge(1L, "PHRASE", "phrase", "phrase content", true, "ACTIVE"));
        state.knowledge.put(2L, knowledge(2L, "TOPIC", "topic", "topic content", true, "ACTIVE"));
        state.knowledge.put(3L, knowledge(3L, "MEME_SCENE", "scene", "scene content", true, "ACTIVE"));
        state.knowledge.put(4L, knowledge(4L, "PHRASE", "low", "low score", true, "ACTIVE"));
        state.knowledge.put(5L, knowledge(5L, "PHRASE", "disabled", "disabled content", false, "DISABLED"));
        state.profiles.put(10L, profile(10L, true, "ACTIVE"));

        Object response = preview(service(state), "ACTIVE_CHAT");
        List<?> items = (List<?>) invoke(response, "items");

        assertThat(items).hasSize(2);
        assertThat(items.stream().map(item -> uncheckedInvoke(item, "type")))
                .containsExactly("PHRASE", "TOPIC");
    }

    @Test
    void simulateDifyInputsIncludesKnowledgeContextWithoutCallingDify() throws Exception {
        State state = new State();
        state.searchResults = List.of(searchResult("GROUP_KNOWLEDGE", 1L, 0.9d));
        state.knowledge.put(1L, knowledge(1L, "PHRASE", "classic", "classic content", true, "ACTIVE"));
        Object service = service(state);
        Object request = cls("com.yh.qqbot.chat.history.dto.DifyContextSimulateRequest")
                .getConstructor(String.class, String.class, String.class, String.class, Integer.class,
                        String.class, String.class, String.class, String.class, String.class)
                .newInstance("251288204", "this is classic", "u1", "PASSIVE_CHAT", 5,
                        "bot", "persona", "recent", null, null);

        Object response = invoke(service, "simulateDifyInputs",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.DifyContextSimulateRequest")},
                request);
        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) invoke(response, "inputs");

        assertThat(inputs.get("text")).isEqualTo("this is classic");
        assertThat(inputs.get("groupId")).isEqualTo("251288204");
        assertThat(inputs.get("userId")).isEqualTo("u1");
        assertThat((String) inputs.get("knowledgeContext")).contains("Reviewed group knowledge only");
        assertThat((String) inputs.get("knowledgeContext")).contains("classic content");
    }

    private Object service(State state) throws Exception {
        Class<?> embeddingServiceType = cls("com.yh.qqbot.chat.history.service.vector.KnowledgeEmbeddingService");
        Class<?> knowledgeMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper");
        Class<?> profileMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper");
        Class<?> propertiesType = cls("com.yh.qqbot.config.properties.QqBotProperties");
        Object embeddingService = mockEmbeddingService(embeddingServiceType, state);
        Object knowledgeMapper = proxy(knowledgeMapperType, state::handleKnowledgeMapper);
        Object profileMapper = proxy(profileMapperType, state::handleProfileMapper);
        Object properties = propertiesType.getConstructor().newInstance();
        return cls("com.yh.qqbot.chat.history.service.context.KnowledgeContextService")
                .getConstructor(embeddingServiceType, knowledgeMapperType, profileMapperType, propertiesType)
                .newInstance(embeddingService, knowledgeMapper, profileMapper, properties);
    }

    private Object mockEmbeddingService(Class<?> type, State state) throws Exception {
        Answer<Object> answer = invocation -> {
            if ("search".equals(invocation.getMethod().getName())) {
                return searchResponse(state.searchResults);
            }
            return defaultValue(invocation.getMethod().getReturnType());
        };
        return Mockito.mock(type, answer);
    }

    private Object preview(Object service, String routeType) throws Exception {
        Object request = cls("com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewRequest")
                .getConstructor(String.class, String.class, String.class, String.class, Integer.class)
                .newInstance("251288204", "this is classic", "u1", routeType, 5);
        return invoke(service, "preview",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewRequest")},
                request);
    }

    private Object searchResponse(List<Object> results) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.KnowledgeSearchResponse")
                .getConstructor(String.class, List.class)
                .newInstance("query", results);
    }

    private Object searchResult(String targetType, Long targetId, double score) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.KnowledgeSearchResult")
                .getConstructor(String.class, Long.class, double.class, String.class, String.class)
                .newInstance(targetType, targetId, score, "title-" + targetId, "content-" + targetId);
    }

    private Object knowledge(Long id, String type, String title, String content, boolean enabled, String status) throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity")
                .getConstructor()
                .newInstance();
        set(entity, "id", id);
        set(entity, "groupId", "251288204");
        set(entity, "knowledgeType", type);
        set(entity, "title", title);
        set(entity, "content", content);
        set(entity, "enabled", enabled);
        set(entity, "status", status);
        return entity;
    }

    private Object profile(Long id, boolean enabled, String status) throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatMemberProfileEntity")
                .getConstructor()
                .newInstance();
        set(entity, "id", id);
        set(entity, "groupId", "251288204");
        set(entity, "senderUid", "u" + id);
        set(entity, "senderName", "member-" + id);
        set(entity, "profileText", "profile text " + id);
        set(entity, "enabled", enabled);
        set(entity, "status", status);
        return entity;
    }

    private Object proxy(Class<?> type, MapperHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.handle(method, args));
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

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object uncheckedInvoke(Object target, String methodName) {
        try {
            return invoke(target, methodName);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
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

    @FunctionalInterface
    private interface MapperHandler {
        Object handle(Method method, Object[] args) throws Exception;
    }

    private static final class State {
        private List<Object> searchResults = List.of();
        private final Map<Long, Object> knowledge = new HashMap<>();
        private final Map<Long, Object> profiles = new HashMap<>();

        private Object handleKnowledgeMapper(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return knowledge.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleProfileMapper(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return profiles.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        }
    }
}
