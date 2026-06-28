package com.yh.qqbot.chat.history.service.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManualKnowledgeCandidateServiceTest {

    @Test
    void manualCandidateInsertSucceedsAndDuplicateDoesNotInsertAgain() throws Exception {
        MapperState state = new MapperState();
        Object service = service(state);

        Object request = request("PHRASE", "典", "典");
        Object first = invoke(service, "addManualCandidate",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest")},
                request);
        Object second = invoke(service, "addManualCandidate",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest")},
                request);

        assertThat(invoke(first, "duplicate")).isEqualTo(false);
        assertThat(invoke(second, "duplicate")).isEqualTo(true);
        assertThat(state.inserted).hasSize(1);
        Object candidate = state.inserted.get(0);
        assertThat(invoke(candidate, "getStatus")).isEqualTo("PENDING");
        assertThat(invoke(candidate, "getHitCount")).isEqualTo(1L);
        assertThat(invoke(candidate, "getMemberCount")).isEqualTo(1L);
    }

    @Test
    void invalidCandidateTypeFails() throws Exception {
        Throwable thrown = thrownByAdd(request("BAD_TYPE", "典", "典"));

        assertThat(thrown).isInstanceOf(cls("com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException"));
        assertThat(thrown).hasMessage("candidateType is invalid");
    }

    @Test
    void emptyContentFails() throws Exception {
        Throwable thrown = thrownByAdd(request("PHRASE", "典", " "));

        assertThat(thrown).isInstanceOf(cls("com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException"));
        assertThat(thrown).hasMessage("content is required");
    }

    private Throwable thrownByAdd(Object request) throws Exception {
        try {
            invoke(service(new MapperState()), "addManualCandidate",
                    new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest")},
                    request);
            return null;
        } catch (InvocationTargetException ex) {
            return ex.getCause();
        }
    }

    private Object service(MapperState state) throws Exception {
        Class<?> mapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper");
        Object mapper = Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[]{mapperType},
                (proxy, method, args) -> state.handle(method, args));
        return cls("com.yh.qqbot.chat.history.service.candidate.ManualKnowledgeCandidateService")
                .getConstructor(mapperType)
                .newInstance(mapper);
    }

    private Object request(String candidateType, String title, String content) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest")
                .getConstructor(Long.class, String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class)
                .newInstance(1L, "251288204", candidateType, title, content,
                        "manual evidence", "tester", "manual");
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

    private static final class MapperState {
        private final List<Object> inserted = new ArrayList<>();

        private Object handle(Method method, Object[] args) {
            if ("selectOne".equals(method.getName())) {
                return inserted.isEmpty() ? null : inserted.get(0);
            }
            if ("insert".equals(method.getName())) {
                inserted.add(args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        }

        private Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
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
}
