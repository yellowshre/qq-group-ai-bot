package com.yh.qqbot.chat.history.service.vector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class KnowledgeEmbeddingServiceTest {

    @Test
    void rankSearchCandidatesOrdersByCosineScore() throws Exception {
        Object service = service(null, null, null, null);
        Class<?> candidateType = cls("com.yh.qqbot.chat.history.service.vector.KnowledgeEmbeddingService$KnowledgeSearchCandidate");
        List<Object> candidates = List.of(
                candidateType.getConstructor(String.class, Long.class, String.class, String.class, List.class)
                        .newInstance("GROUP_KNOWLEDGE", 1L, "far", "far content", List.of(0.0d, 1.0d)),
                candidateType.getConstructor(String.class, Long.class, String.class, String.class, List.class)
                        .newInstance("GROUP_KNOWLEDGE", 2L, "near", "near content", List.of(1.0d, 0.0d)));

        List<?> results = (List<?>) invoke(service, "rankSearchCandidates",
                new Class<?>[]{List.class, List.class, int.class},
                candidates, List.of(1.0d, 0.0d), 2);

        assertThat(results).hasSize(2);
        assertThat(invoke(results.get(0), "targetId")).isEqualTo(2L);
        assertThat(invoke(results.get(0), "score")).isEqualTo(1.0d);
    }

    @Test
    void generateHandlesOllamaUnavailableWithoutThrowing() throws Exception {
        Class<?> knowledgeMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper");
        Class<?> profileMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper");
        Class<?> embeddingMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeEmbeddingMapper");
        Object knowledgeMapper = proxy(knowledgeMapperType, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return List.of(knowledge());
            }
            return defaultValue(method.getReturnType());
        });
        Object profileMapper = proxy(profileMapperType, (method, args) -> defaultValue(method.getReturnType()));
        CapturingEmbeddingMapper capturing = new CapturingEmbeddingMapper();
        Object embeddingMapper = proxy(embeddingMapperType, capturing::handle);
        Object embeddingService = failingEmbeddingService();
        Object service = service(knowledgeMapper, profileMapper, embeddingMapper, embeddingService);
        Object request = cls("com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateRequest")
                .getConstructor(String.class, List.class, Boolean.class)
                .newInstance("251288204", List.of("GROUP_KNOWLEDGE"), false);

        Object response = invoke(service, "generate",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateRequest")},
                request);

        assertThat(invoke(response, "embedded")).isEqualTo(0L);
        assertThat(invoke(response, "failed")).isEqualTo(1L);
        assertThat(capturing.inserted).hasSize(1);
        Object failed = capturing.inserted.get(0);
        assertThat(invoke(failed, "getStatus")).isEqualTo("FAILED");
        assertThat(invoke(failed, "getEmbeddingText")).isNull();
        assertThat((String) invoke(failed, "getErrorMessage")).contains("ollama down");
    }

    private Object service(Object knowledgeMapper, Object profileMapper, Object embeddingMapper, Object embeddingService) throws Exception {
        Class<?> knowledgeMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper");
        Class<?> profileMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper");
        Class<?> embeddingMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeEmbeddingMapper");
        Class<?> embeddingServiceType = cls("com.yh.qqbot.chat.history.service.vector.EmbeddingService");
        return cls("com.yh.qqbot.chat.history.service.vector.KnowledgeEmbeddingService")
                .getConstructor(knowledgeMapperType, profileMapperType, embeddingMapperType, embeddingServiceType, ObjectMapper.class)
                .newInstance(knowledgeMapper, profileMapper, embeddingMapper, embeddingService, new ObjectMapper());
    }

    private Object failingEmbeddingService() throws Exception {
        Class<?> type = cls("com.yh.qqbot.chat.history.service.vector.EmbeddingService");
        Answer<Object> answer = invocation -> switch (invocation.getMethod().getName()) {
            case "enabled" -> true;
            case "model" -> "bge-m3";
            case "embed" -> throwFailure();
            default -> defaultValue(invocation.getMethod().getReturnType());
        };
        return Mockito.mock(type, answer);
    }

    private Object throwFailure() {
        throw new IllegalStateException("ollama down");
    }

    private Object knowledge() throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity")
                .getConstructor()
                .newInstance();
        set(entity, "id", 1L);
        set(entity, "groupId", "251288204");
        set(entity, "knowledgeType", "PHRASE");
        set(entity, "title", "classic");
        set(entity, "content", "classic content");
        set(entity, "evidenceText", "evidence");
        set(entity, "status", "ACTIVE");
        set(entity, "enabled", true);
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

    private static final class CapturingEmbeddingMapper {
        private final List<Object> inserted = new ArrayList<>();

        private Object handle(Method method, Object[] args) {
            if ("selectOne".equals(method.getName())) {
                return null;
            }
            if ("insert".equals(method.getName())) {
                inserted.add(args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        }
    }
}
