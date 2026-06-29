package com.yh.qqbot.chat.history.service.rank;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatMemberRankServiceTest {

    @Test
    void totalRankUsesLatestBatchAndClampsTopN() throws Exception {
        Fixture fixture = fixture();
        Object request = request("251288204", null, "message", null, null, 99);

        Object response = invoke(fixture.service(), "rank",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.ChatMemberRankRequest")},
                request);

        assertThat(invoke(response, "batchId")).isEqualTo(7L);
        assertThat(invoke(response, "topN")).isEqualTo(3);
        List<?> items = (List<?>) invoke(response, "items");
        assertThat(items).hasSize(3);
        assertThat(invoke(items.get(0), "senderName")).isEqualTo("alice");
        assertThat(invoke(items.get(0), "score")).isEqualTo(20L);
        assertThat(invoke(items.get(1), "senderName")).isEqualTo("bob");
        assertThat(invoke(items.get(1), "score")).isEqualTo(12L);
    }

    @Test
    void dailyRankAggregatesDateRange() throws Exception {
        Fixture fixture = fixture();
        Object request = request(
                "251288204",
                7L,
                "reply",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 2),
                5);

        Object response = invoke(fixture.service(), "rank",
                new Class<?>[]{cls("com.yh.qqbot.chat.history.dto.ChatMemberRankRequest")},
                request);

        assertThat(invoke(response, "topN")).isEqualTo(3);
        List<?> items = (List<?>) invoke(response, "items");
        assertThat(items).hasSize(2);
        assertThat(invoke(items.get(0), "senderName")).isEqualTo("alice");
        assertThat(invoke(items.get(0), "score")).isEqualTo(7L);
        assertThat(invoke(items.get(0), "messageCount")).isEqualTo(15L);
        assertThat(invoke(items.get(1), "senderName")).isEqualTo("bob");
    }

    private Fixture fixture() throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object memberRank = invoke(properties, "getMemberRank");
        invoke(memberRank, "setDefaultTopN", new Class<?>[]{int.class}, 2);
        invoke(memberRank, "setMaxTopN", new Class<?>[]{int.class}, 3);
        Object service = cls("com.yh.qqbot.chat.history.service.rank.ChatMemberRankService")
                .getConstructor(
                        cls("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper"),
                        cls("com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper"),
                        cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(
                        mapper("com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper"),
                        mapper("com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper"),
                        properties);
        return new Fixture(service);
    }

    private Object mapper(String className) throws Exception {
        Class<?> type = cls(className);
        InvocationHandler handler = (proxy, method, args) -> handleMapperMethod(className, method);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private Object handleMapperMethod(String className, Method method) throws Exception {
        if ("selectOne".equals(method.getName())
                && className.endsWith("ChatImportBatchMapper")) {
            Object batch = cls("com.yh.qqbot.chat.history.entity.ChatImportBatchEntity")
                    .getConstructor()
                    .newInstance();
            invoke(batch, "setId", new Class<?>[]{Long.class}, 7L);
            invoke(batch, "setGroupId", new Class<?>[]{String.class}, "251288204");
            invoke(batch, "setStatus", new Class<?>[]{String.class}, "SUCCESS");
            return batch;
        }
        if ("selectList".equals(method.getName())
                && className.endsWith("ChatMemberStatMapper")) {
            return List.of(
                    total("u1", "10001", "alice", 30L, 20L, 3, 1L, 2L, 4L, 5L),
                    total("u2", "10002", "bob", 16L, 12L, 2, 0L, 3L, 1L, 2L),
                    total("u3", "10003", "carl", 10L, 7L, 1, 0L, 1L, 0L, 1L),
                    total("u4", "10004", "dora", 8L, 1L, 1, 0L, 0L, 0L, 1L));
        }
        if ("selectList".equals(method.getName())
                && className.endsWith("ChatMemberStatDailyMapper")) {
            return List.of(
                    daily("u1", "10001", "alice", LocalDate.of(2026, 6, 1), 10L, 1L, 5L, 2L, 1L),
                    daily("u1", "10001", "alice", LocalDate.of(2026, 6, 2), 5L, 1L, 2L, 3L, 1L),
                    daily("u2", "10002", "bob", LocalDate.of(2026, 6, 1), 8L, 1L, 4L, 1L, 2L));
        }
        if ("toString".equals(method.getName())) {
            return "rankMapper";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(method.getName())) {
            return false;
        }
        return null;
    }

    private Object total(
            String uid,
            String uin,
            String name,
            Long raw,
            Long messages,
            Integer activeDays,
            Long mentions,
            Long replies,
            Long repliedBy,
            Long sessions) throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatMemberStatEntity").getConstructor().newInstance();
        invoke(entity, "setSenderUid", new Class<?>[]{String.class}, uid);
        invoke(entity, "setSenderUin", new Class<?>[]{String.class}, uin);
        invoke(entity, "setSenderName", new Class<?>[]{String.class}, name);
        invoke(entity, "setRawMessageCount", new Class<?>[]{Long.class}, raw);
        invoke(entity, "setMessageCount", new Class<?>[]{Long.class}, messages);
        invoke(entity, "setActiveDays", new Class<?>[]{Integer.class}, activeDays);
        invoke(entity, "setMentionCount", new Class<?>[]{Long.class}, mentions);
        invoke(entity, "setReplyCount", new Class<?>[]{Long.class}, replies);
        invoke(entity, "setRepliedByCount", new Class<?>[]{Long.class}, repliedBy);
        invoke(entity, "setSessionCount", new Class<?>[]{Long.class}, sessions);
        return entity;
    }

    private Object daily(
            String uid,
            String uin,
            String name,
            LocalDate date,
            Long messages,
            Long activeDays,
            Long replies,
            Long repliedBy,
            Long sessions) throws Exception {
        Object entity = cls("com.yh.qqbot.chat.history.entity.ChatMemberStatDailyEntity")
                .getConstructor()
                .newInstance();
        invoke(entity, "setSenderUid", new Class<?>[]{String.class}, uid);
        invoke(entity, "setSenderUin", new Class<?>[]{String.class}, uin);
        invoke(entity, "setSenderName", new Class<?>[]{String.class}, name);
        invoke(entity, "setStatDate", new Class<?>[]{LocalDate.class}, date);
        invoke(entity, "setRawMessageCount", new Class<?>[]{Long.class}, messages);
        invoke(entity, "setMessageCount", new Class<?>[]{Long.class}, messages);
        invoke(entity, "setActiveDays", new Class<?>[]{Integer.class}, activeDays.intValue());
        invoke(entity, "setMentionCount", new Class<?>[]{Long.class}, 0L);
        invoke(entity, "setReplyCount", new Class<?>[]{Long.class}, replies);
        invoke(entity, "setRepliedByCount", new Class<?>[]{Long.class}, repliedBy);
        invoke(entity, "setSessionCount", new Class<?>[]{Long.class}, sessions);
        return entity;
    }

    private Object request(
            String groupId,
            Long batchId,
            String rankType,
            LocalDate startDate,
            LocalDate endDate,
            Integer topN) throws Exception {
        return cls("com.yh.qqbot.chat.history.dto.ChatMemberRankRequest")
                .getConstructor(String.class, Long.class, String.class, LocalDate.class, LocalDate.class, Integer.class)
                .newInstance(groupId, batchId, rankType, startDate, endDate, topN);
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

    private record Fixture(Object service) {
    }
}
