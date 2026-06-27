package com.yh.qqbot.service.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BotSafetyWordServiceReflectionTest {

    @Test
    void activeChatOffWordsAreMatchedFirst() throws Exception {
        Object result = invoke(service(), "match", new Class<?>[]{String.class}, "小黄闭嘴");

        assertThat(invoke(result, "matched")).isEqualTo(true);
        assertThat(invoke(result, "action")).isEqualTo("ACTIVE_CHAT_OFF");
        assertThat(invoke(result, "matchedWord")).isEqualTo("小黄闭嘴");
        assertThat(invoke(result, "adminOnly")).isEqualTo(true);
    }

    @Test
    void autoChatOffCommandMatchesOffAction() throws Exception {
        Object result = invoke(service(), "match", new Class<?>[]{String.class}, "#autochatoff");

        assertThat(invoke(result, "action")).isEqualTo("ACTIVE_CHAT_OFF");
    }

    @Test
    void activeChatOnWordsAreMatched() throws Exception {
        Object result = invoke(service(), "match", new Class<?>[]{String.class}, "小黄说话");

        assertThat(invoke(result, "matched")).isEqualTo(true);
        assertThat(invoke(result, "action")).isEqualTo("ACTIVE_CHAT_ON");
    }

    @Test
    void normalChatDoesNotMatchSafetyWords() throws Exception {
        Object result = invoke(service(), "match", new Class<?>[]{String.class}, "普通聊天内容");

        assertThat(invoke(result, "matched")).isEqualTo(false);
        assertThat(invoke(result, "action")).isEqualTo("NONE");
    }

    @Test
    void safetyWordCanWinWhenItAlsoContainsPassiveTriggerWord() throws Exception {
        Object properties = properties();
        Object passiveChat = invoke(properties, "getPassiveChat");
        invoke(passiveChat, "setTriggerWords", new Class<?>[]{List.class}, List.of("小黄"));
        Object service = newService(properties);

        Object result = invoke(service, "match", new Class<?>[]{String.class}, "小黄闭嘴");

        assertThat(invoke(result, "action")).isEqualTo("ACTIVE_CHAT_OFF");
    }

    private Object service() throws Exception {
        return newService(properties());
    }

    private Object newService(Object properties) throws Exception {
        return cls("com.yh.qqbot.service.bot.BotSafetyWordService")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
    }

    private Object properties() throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object safety = invoke(properties, "getSafety");
        invoke(safety, "setAdminOnly", new Class<?>[]{boolean.class}, true);
        invoke(safety, "setActiveChatOffWords", new Class<?>[]{List.class}, List.of("小黄闭嘴", "#autochatoff"));
        invoke(safety, "setActiveChatOnWords", new Class<?>[]{List.class}, List.of("小黄说话", "#autochaton"));
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
}
