package com.yh.qqbot.chat.history.service.formal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormalKnowledgeServiceTest {

    @Test
    void publishKnowledgeOnlyPublishesApprovedAndSkipsDuplicate() throws Exception {
        State state = new State();
        state.knowledgeCandidates.put(1L, knowledgeCandidate(1L, "APPROVED"));
        state.knowledgeCandidates.put(2L, knowledgeCandidate(2L, "PENDING"));
        Object service = service(state);

        Object first = invoke(service, "publishKnowledge",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest")},
                request(List.of(1L, 2L)));
        Object second = invoke(service, "publishKnowledge",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest")},
                request(List.of(1L)));

        assertThat(invoke(first, "published")).isEqualTo(1L);
        assertThat(invoke(first, "skipped")).isEqualTo(1L);
        assertThat(invoke(second, "published")).isEqualTo(0L);
        assertThat(invoke(second, "skipped")).isEqualTo(1L);
        assertThat(state.groupKnowledgeInserted).hasSize(1);
        Object inserted = state.groupKnowledgeInserted.get(0);
        assertThat(invoke(inserted, "getStatus")).isEqualTo("ACTIVE");
        assertThat(invoke(inserted, "getEnabled")).isEqualTo(true);
    }

    @Test
    void publishMemberProfileBuildsProfileText() throws Exception {
        State state = new State();
        state.memberCandidates.put(9L, memberCandidate());
        Object service = service(state);

        Object response = invoke(service, "publishMemberProfiles",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest")},
                request(List.of(9L)));

        assertThat(invoke(response, "published")).isEqualTo(1L);
        assertThat(state.memberProfilesInserted).hasSize(1);
        Object profile = state.memberProfilesInserted.get(0);
        assertThat((String) invoke(profile, "getProfileText"))
                .contains("messages=12")
                .contains("Reason:");
    }

    private Object service(State state) throws Exception {
        Class<?> knowledgeCandidateMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper");
        Class<?> memberCandidateMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper");
        Class<?> groupKnowledgeMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper");
        Class<?> memberProfileMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper");
        Class<?> logMapperType = cls("com.yh.qqbot.chat.history.mapper.ChatKnowledgePublishLogMapper");
        Object knowledgeCandidateMapper = proxy(knowledgeCandidateMapperType, state::handleKnowledgeCandidate);
        Object memberCandidateMapper = proxy(memberCandidateMapperType, state::handleMemberCandidate);
        Object groupKnowledgeMapper = proxy(groupKnowledgeMapperType, state::handleGroupKnowledge);
        Object memberProfileMapper = proxy(memberProfileMapperType, state::handleMemberProfile);
        Object logMapper = proxy(logMapperType, state::handleLog);
        return cls("com.yh.qqbot.chat.history.service.formal.FormalKnowledgeService")
                .getConstructor(
                        knowledgeCandidateMapperType,
                        memberCandidateMapperType,
                        groupKnowledgeMapperType,
                        memberProfileMapperType,
                        logMapperType)
                .newInstance(
                        knowledgeCandidateMapper,
                        memberCandidateMapper,
                        groupKnowledgeMapper,
                        memberProfileMapper,
                        logMapper);
    }

    private Object request(List<Long> ids) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest")
                .getConstructor(String.class, List.class, String.class, String.class)
                .newInstance("251288204", ids, "tester", "ok");
    }

    private Object knowledgeCandidate(Long id, String status) throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity")
                .getConstructor()
                .newInstance();
        set(entity, "id", id);
        set(entity, "groupId", "251288204");
        set(entity, "candidateType", "PHRASE");
        set(entity, "title", "classic");
        set(entity, "content", "classic");
        set(entity, "evidenceText", "sample evidence");
        set(entity, "status", status);
        return entity;
    }

    private Object memberCandidate() throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity")
                .getConstructor()
                .newInstance();
        set(entity, "id", 9L);
        set(entity, "groupId", "251288204");
        set(entity, "senderUid", "u9");
        set(entity, "senderUin", "10009");
        set(entity, "senderName", "member-nine");
        set(entity, "messageCount", 12L);
        set(entity, "rawMessageCount", 13L);
        set(entity, "activeDays", 3);
        set(entity, "mentionCount", 2L);
        set(entity, "replyCount", 4L);
        set(entity, "repliedByCount", 5L);
        set(entity, "sessionCount", 6L);
        set(entity, "candidateReason", "active member");
        set(entity, "status", "APPROVED");
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
        private final Map<Long, Object> knowledgeCandidates = new java.util.HashMap<>();
        private final Map<Long, Object> memberCandidates = new java.util.HashMap<>();
        private final List<Object> groupKnowledgeInserted = new ArrayList<>();
        private final List<Object> memberProfilesInserted = new ArrayList<>();
        private final List<Object> logs = new ArrayList<>();

        private Object handleKnowledgeCandidate(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return knowledgeCandidates.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleMemberCandidate(Method method, Object[] args) {
            if ("selectById".equals(method.getName())) {
                return memberCandidates.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleGroupKnowledge(Method method, Object[] args) throws Exception {
            if ("selectOne".equals(method.getName())) {
                return groupKnowledgeInserted.isEmpty() ? null : groupKnowledgeInserted.get(0);
            }
            if ("insert".equals(method.getName())) {
                Object entity = args[0];
                set(entity, "id", 100L + groupKnowledgeInserted.size());
                groupKnowledgeInserted.add(entity);
                return 1;
            }
            return defaultValue(method.getReturnType());
        }

        private Object handleMemberProfile(Method method, Object[] args) throws Exception {
            if ("selectOne".equals(method.getName())) {
                return memberProfilesInserted.isEmpty() ? null : memberProfilesInserted.get(0);
            }
            if ("insert".equals(method.getName())) {
                Object entity = args[0];
                set(entity, "id", 200L + memberProfilesInserted.size());
                memberProfilesInserted.add(entity);
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
