package com.yh.qqbot.service.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BotIdentityServiceReflectionTest {

    @Test
    void aliasMatchedWhenMessageContainsConfiguredAlias() throws Exception {
        Object service = service();

        assertThat(invoke(service, "isBotAliasMatched", new Class<?>[]{String.class}, "小黄你怎么看")).isEqualTo(true);
        assertThat(invoke(service, "isBotAliasMatched", new Class<?>[]{String.class}, "黄哥这波怎么说")).isEqualTo(true);
    }

    @Test
    void normalOrBlankMessageDoesNotMatchAlias() throws Exception {
        Object service = service();

        assertThat(invoke(service, "isBotAliasMatched", new Class<?>[]{String.class}, "今天晚上吃什么")).isEqualTo(false);
        assertThat(invoke(service, "isBotAliasMatched", new Class<?>[]{String.class}, " ")).isEqualTo(false);
        assertThat(invoke(service, "isBotAliasMatched", new Class<?>[]{String.class}, new Object[]{null})).isEqualTo(false);
    }

    @Test
    void passiveTriggerWordsAreConfiguredSeparately() throws Exception {
        Object properties = properties();
        Object passiveChat = invoke(properties, "getPassiveChat");
        invoke(passiveChat, "setTriggerWords", new Class<?>[]{List.class}, List.of("小黄", "来聊聊"));
        Object service = newService(properties);

        assertThat(invoke(service, "isPassiveTriggerMatched", new Class<?>[]{String.class}, "小黄你在吗")).isEqualTo(true);
        assertThat(invoke(service, "isPassiveTriggerMatched", new Class<?>[]{String.class}, "来聊聊这个方案")).isEqualTo(true);
        assertThat(invoke(service, "isPassiveTriggerMatched", new Class<?>[]{String.class}, "普通消息")).isEqualTo(false);
    }

    private Object service() throws Exception {
        return newService(properties());
    }

    private Object newService(Object properties) throws Exception {
        return cls("com.yh.qqbot.service.bot.BotIdentityService")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
    }

    private Object properties() throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object identity = invoke(properties, "getIdentity");
        invoke(identity, "setDisplayName", new Class<?>[]{String.class}, "小黄");
        invoke(identity, "setAliases", new Class<?>[]{List.class}, List.of("小黄", "黄哥"));
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
