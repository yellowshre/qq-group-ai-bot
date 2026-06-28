package com.yh.qqbot.chat.history.service.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatKnowledgeReviewServiceTest {

    @Test
    void reviewKnowledgeCandidateUpdatesStatusAndWritesLog() throws Exception {
        State state = new State();
        Object candidate = cls("com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity")
                .getConstructor()
                .newInstance();
        set(candidate, "id", 9L);
        set(candidate, "status", "PENDING");
        state.knowledgeCandidate = candidate;

        Object reviewed = invoke(service(state), "reviewKnowledgeCandidate",
                new Class<?>[]{Long.class, cls("com.yh.qqbot.chat.history.dto.CandidateReviewRequest")},
                9L, reviewRequest("APPROVED"));

        assertThat(invoke(reviewed, "getStatus")).isEqualTo("APPROVED");
        assertThat(state.updatedKnowledge).isSameAs(candidate);
        assertThat(state.logs).hasSize(1);
        Object log = state.logs.get(0);
        assertThat(invoke(log, "getTargetType")).isEqualTo("KNOWLEDGE_CANDIDATE");
        assertThat(invoke(log, "getOldStatus")).isEqualTo("PENDING");
        assertThat(invoke(log, "getNewStatus")).isEqualTo("APPROVED");
    }

    private Object service(State state) throws Exception {
        Class<?> knowledgeMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper");
        Class<?> memberMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper");
        Class<?> logMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeReviewLogMapper");
        Object knowledgeMapper = Proxy.newProxyInstance(knowledgeMapperType.getClassLoader(), new Class<?>[]{knowledgeMapperType},
                (proxy, method, args) -> state.handleKnowledge(method, args));
        Object memberMapper = Proxy.newProxyInstance(memberMapperType.getClassLoader(), new Class<?>[]{memberMapperType},
                (proxy, method, args) -> state.handleMember(method, args));
        Object logMapper = Proxy.newProxyInstance(logMapperType.getClassLoader(), new Class<?>[]{logMapperType},
                (proxy, method, args) -> state.handleLog(method, args));
        return cls("com.yh.qqbot.chat.history.service.review.ChatKnowledgeReviewService")
                .getConstructor(knowledgeMapperType, memberMapperType, logMapperType)
                .newInstance(knowledgeMapper, memberMapper, logMapper);
    }

    private Object reviewRequest(String status) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.CandidateReviewRequest")
                .getConstructor(String.class, String.class, String.class)
                .newInstance(status, "tester", "ok");
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

    private static final class State {
        private Object knowledgeCandidate;
        private Object memberCandidate;
        private Object updatedKnowledge;
        private Object updatedMember;
        private final List<Object> logs = new ArrayList<>();

        private Object handleKnowledge(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return knowledgeCandidate;
            }
            if ("updateById".equals(method.getName())) {
                updatedKnowledge = args[0];
                return 1;
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleMember(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return memberCandidate;
            }
            if ("updateById".equals(method.getName())) {
                updatedMember = args[0];
                return 1;
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleLog(Method method, Object[] args) {
            if ("insert".equals(method.getName())) {
                logs.add(args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        }
    }
}
