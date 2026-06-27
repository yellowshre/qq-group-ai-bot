package com.yh.qqbot.service.active;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ActiveChatPolicyServiceReflectionTest {

    @Test
    void rejectsWhenGlobalActiveChatDisabled() throws Exception {
        Fixture fixture = fixture();
        set(invoke(fixture.properties(), "getActiveChat"), "setEnabled", boolean.class, false);

        Object result = evaluate(fixture.service(), request());

        assertRejected(result, "ACTIVE_CHAT_DISABLED");
    }

    @Test
    void rejectsWhenAtBot() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.atBot = true)), "AT_BOT");
    }

    @Test
    void rejectsWhenBotNicknameMatched() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.botNicknameMatched = true)), "BOT_NICKNAME_MATCHED");
    }

    @Test
    void rejectsWhenGroupBotDisabled() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.groupBotEnabled = false)), "GROUP_DISABLED");
    }

    @Test
    void rejectsWhenGroupActiveChatDisabled() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.activeChatEnabledInGroup = false)), "GROUP_ACTIVE_CHAT_DISABLED");
    }

    @Test
    void rejectsWhenAdminCommandHit() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.adminCommandHit = true)), "ADMIN_COMMAND");
    }

    @Test
    void rejectsWhenMessageIsEmpty() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.rawMessage = " ")), "EMPTY_MESSAGE");
    }

    @Test
    void rejectsWhenMessageTooShort() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.rawMessage = "hi")), "TOO_SHORT");
    }

    @Test
    void rejectsWhenMessageIsPunctuationOnly() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.rawMessage = "！！！")), "PUNCTUATION_ONLY");
    }

    @Test
    void rejectsWhenMessageTooLong() throws Exception {
        Fixture fixture = fixture();
        set(invoke(fixture.properties(), "getActiveChat"), "setMaxMessageLength", int.class, 5);

        Object result = evaluate(fixture.service(), request(builder -> builder.rawMessage = "123456"));

        assertRejected(result, "TOO_LONG");
    }

    @Test
    void rejectsWhenMemeAlreadySentAndNotAllowed() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.memeAlreadySent = true)), "MEME_ALREADY_SENT");
    }

    @Test
    void rejectsWhenLastMessageFromBotAndNotAllowed() throws Exception {
        assertRejected(evaluate(fixture().service(), request(builder -> builder.lastMessageFromBot = true)), "LAST_MESSAGE_FROM_BOT");
    }

    @Test
    void rejectsWhenRandomProbabilityIsZero() throws Exception {
        Fixture fixture = fixture();
        set(invoke(fixture.properties(), "getActiveChat"), "setRandomProbability", double.class, 0.0);

        Object result = evaluate(fixture.service(), request());

        assertRejected(result, "RANDOM_MISS");
        assertThat(invoke(result, "randomHit")).isEqualTo(false);
    }

    @Test
    void allowsWhenRandomProbabilityIsOneAndOtherConditionsPass() throws Exception {
        Object result = evaluate(fixture().service(), request());

        assertThat(invoke(result, "allowed")).isEqualTo(true);
        assertThat(invoke(result, "rejectReason")).isEqualTo("ALLOWED");
    }

    @Test
    void rejectsWhenCooldownKeyExists() throws Exception {
        Fixture fixture = fixture();
        when(fixture.redis().hasKey("qqbot:active:cooldown:10001")).thenReturn(true);

        Object result = evaluate(fixture.service(), request());

        assertRejected(result, "COOLDOWN");
    }

    @Test
    void rejectsWhenHourlyLimitReached() throws Exception {
        Fixture fixture = fixture();
        when(fixture.valueOps().get(startsWith("qqbot:active:hour:10001:"))).thenReturn("20");

        Object result = evaluate(fixture.service(), request());

        assertRejected(result, "HOURLY_LIMIT");
    }

    @Test
    void markActiveChatSentWritesCooldownAndHourlyCounter() throws Exception {
        Fixture fixture = fixture();
        when(fixture.valueOps().increment(anyString())).thenReturn(1L);

        invoke(fixture.service(), "markActiveChatSent", new Class<?>[]{Long.class}, 10001L);

        verify(fixture.valueOps()).set("qqbot:active:cooldown:10001", "1", Duration.ofSeconds(180));
        verify(fixture.valueOps()).increment(startsWith("qqbot:active:hour:10001:"));
        verify(fixture.redis()).expire(startsWith("qqbot:active:hour:10001:"), eq(Duration.ofHours(2)));
    }

    private void assertRejected(Object result, String rejectReason) throws Exception {
        assertThat(invoke(result, "allowed")).isEqualTo(false);
        assertThat(invoke(result, "rejectReason")).isEqualTo(rejectReason);
    }

    private Object evaluate(Object service, Object request) throws Exception {
        return invoke(service, "evaluate", new Class<?>[]{cls("com.yh.qqbot.dto.ActiveChatPolicyRequest")}, request);
    }

    private Object request() throws Exception {
        return request(builder -> {
        });
    }

    private Object request(java.util.function.Consumer<RequestBuilder> customizer) throws Exception {
        RequestBuilder builder = new RequestBuilder();
        customizer.accept(builder);
        return cls("com.yh.qqbot.dto.ActiveChatPolicyRequest")
                .getConstructor(Long.class, Long.class, String.class, boolean.class, boolean.class,
                        boolean.class, boolean.class, boolean.class, boolean.class, boolean.class)
                .newInstance(
                        10001L,
                        20001L,
                        builder.rawMessage,
                        builder.atBot,
                        builder.botNicknameMatched,
                        builder.groupBotEnabled,
                        builder.activeChatEnabledInGroup,
                        builder.adminCommandHit,
                        builder.memeAlreadySent,
                        builder.lastMessageFromBot);
    }

    private Fixture fixture() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(anyString())).thenReturn(false);
        when(valueOps.get(anyString())).thenReturn(null);

        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object activeChat = invoke(properties, "getActiveChat");
        set(activeChat, "setEnabled", boolean.class, true);
        set(activeChat, "setCooldownSeconds", long.class, 180L);
        set(activeChat, "setMaxPerHour", long.class, 20L);
        set(activeChat, "setRandomProbability", double.class, 1.0);
        set(activeChat, "setMinMessageLength", int.class, 3);
        set(activeChat, "setMaxMessageLength", int.class, 80);
        set(activeChat, "setAllowAfterMemeSent", boolean.class, false);
        set(activeChat, "setAllowAfterBotMessage", boolean.class, false);

        Object service = cls("com.yh.qqbot.service.active.ActiveChatPolicyService")
                .getConstructor(StringRedisTemplate.class, cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(redis, properties);
        return new Fixture(service, redis, valueOps, properties);
    }

    private void set(Object target, String methodName, Class<?> type, Object value) throws Exception {
        invoke(target, methodName, new Class<?>[]{type}, value);
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

    private record Fixture(
            Object service,
            StringRedisTemplate redis,
            ValueOperations<String, String> valueOps,
            Object properties) {
    }

    private static class RequestBuilder {
        private String rawMessage = "今天晚上吃什么比较合适";
        private boolean atBot = false;
        private boolean botNicknameMatched = false;
        private boolean groupBotEnabled = true;
        private boolean activeChatEnabledInGroup = true;
        private boolean adminCommandHit = false;
        private boolean memeAlreadySent = false;
        private boolean lastMessageFromBot = false;
    }
}
