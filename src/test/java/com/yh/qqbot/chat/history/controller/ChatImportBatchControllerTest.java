package com.yh.qqbot.chat.history.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatImportBatchControllerTest {

    @Test
    void importBatchesReturnsSafeSummaries() throws Exception {
        Object entity = batchEntity();
        Object mapper = mapper(entity);
        Object controller = cls("com.yh.qqbot.chat.history.controller.ChatImportBatchController")
                .getConstructor(cls("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper"))
                .newInstance(mapper);

        Object result = controller.getClass()
                .getMethod("importBatches", String.class, String.class, Integer.class)
                .invoke(controller, " 251288204 ", " SUCCESS ", 500);

        assertThat(result).isInstanceOf(List.class);
        List<?> summaries = (List<?>) result;
        assertThat(summaries).hasSize(1);
        Object summary = summaries.get(0);
        assertThat(invoke(summary, "batchId")).isEqualTo(7L);
        assertThat(invoke(summary, "groupId")).isEqualTo("251288204");
        assertThat(invoke(summary, "sourceFile")).isEqualTo("data/chat-export/group_251288204_sample.json");
        assertThat(invoke(summary, "rawCount")).isEqualTo(416L);
        assertThat(invoke(summary, "cleanCount")).isEqualTo(259L);
        assertThat(invoke(summary, "status")).isEqualTo("SUCCESS");
        assertThat((String) invoke(summary, "errorMessage")).hasSize(243).endsWith("...");
    }

    private Object mapper(Object entity) throws Exception {
        Class<?> mapperType = cls("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper");
        return Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[]{mapperType},
                (proxy, method, args) -> "selectList".equals(method.getName())
                        ? List.of(entity)
                        : defaultValue(method.getReturnType()));
    }

    private Object batchEntity() throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatImportBatchEntity")
                .getConstructor()
                .newInstance();
        set(entity, "setId", Long.class, 7L);
        set(entity, "setGroupId", String.class, "251288204");
        set(entity, "setSourceFile", String.class, "data/chat-export/group_251288204_sample.json");
        set(entity, "setSourceHash", String.class, "source-hash-should-not-be-returned");
        set(entity, "setChatName", String.class, "sample group");
        set(entity, "setExporterName", String.class, "NapCat-QCE");
        set(entity, "setExporterVersion", String.class, "1.0");
        set(entity, "setStartTime", LocalDateTime.class, LocalDateTime.of(2026, 6, 1, 0, 0));
        set(entity, "setEndTime", LocalDateTime.class, LocalDateTime.of(2026, 6, 28, 23, 59));
        set(entity, "setTotalMessages", Long.class, 416L);
        set(entity, "setRawCount", Long.class, 416L);
        set(entity, "setCleanCount", Long.class, 259L);
        set(entity, "setMentionCount", Long.class, 39L);
        set(entity, "setReplyCount", Long.class, 28L);
        set(entity, "setSessionCount", Long.class, 10L);
        set(entity, "setMemberCount", Long.class, 17L);
        set(entity, "setStatus", String.class, "SUCCESS");
        set(entity, "setErrorMessage", String.class, "x".repeat(260));
        set(entity, "setCreatedAt", LocalDateTime.class, LocalDateTime.of(2026, 6, 30, 20, 0));
        set(entity, "setUpdatedAt", LocalDateTime.class, LocalDateTime.of(2026, 6, 30, 20, 1));
        return entity;
    }

    private Object invoke(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }

    private void set(Object target, String method, Class<?> type, Object value) throws Exception {
        target.getClass().getMethod(method, type).invoke(target, value);
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
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
