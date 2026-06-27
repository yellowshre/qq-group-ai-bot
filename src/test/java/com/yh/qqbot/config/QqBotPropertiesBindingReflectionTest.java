package com.yh.qqbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class QqBotPropertiesBindingReflectionTest {

    @Test
    void bindsIdentitySafetyAndActiveChatProperties() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("qqbot.identity.display-name", "小黄")
                .withProperty("qqbot.identity.aliases[0]", "小黄")
                .withProperty("qqbot.identity.aliases[1]", "黄哥")
                .withProperty("qqbot.safety.active-chat-off-words[0]", "小黄闭嘴")
                .withProperty("qqbot.safety.active-chat-on-words[0]", "小黄说话")
                .withProperty("qqbot.active-chat.cooldown-seconds", "180")
                .withProperty("qqbot.active-chat.max-per-hour", "20")
                .withProperty("qqbot.active-chat.random-probability", "0.75")
                .withProperty("qqbot.active-chat.min-confidence", "0.6")
                .withProperty("qqbot.dify.workflow.meme-scene", "meme-scene-recognizer")
                .withProperty("qqbot.dify.workflow.passive-chat", "passive-chat-reply")
                .withProperty("qqbot.dify.workflow.active-chat", "active-chat-reply")
                .withProperty("qqbot.dify.meme-scene-api-key", "test-meme-key")
                .withProperty("qqbot.dify.passive-chat-api-key", "test-passive-key")
                .withProperty("qqbot.dify.active-chat-api-key", "test-active-key");

        Object properties = Binder.get(env).bind("qqbot", Bindable.of(cls("com.yh.qqbot.config.properties.QqBotProperties"))).get();
        Object identity = invoke(properties, "getIdentity");
        Object safety = invoke(properties, "getSafety");
        Object activeChat = invoke(properties, "getActiveChat");
        Object dify = invoke(properties, "getDify");

        assertThat(invoke(identity, "getDisplayName")).isEqualTo("小黄");
        assertThat((java.util.List<Object>) invoke(identity, "getAliases")).containsExactly("小黄", "黄哥");
        assertThat((java.util.List<Object>) invoke(safety, "getActiveChatOffWords")).containsExactly("小黄闭嘴");
        assertThat((java.util.List<Object>) invoke(safety, "getActiveChatOnWords")).containsExactly("小黄说话");
        assertThat(invoke(activeChat, "getCooldownSeconds")).isEqualTo(180L);
        assertThat(invoke(activeChat, "getMaxPerHour")).isEqualTo(20L);
        assertThat(invoke(activeChat, "getRandomProbability")).isEqualTo(0.75);
        assertThat(invoke(activeChat, "getMinConfidence")).isEqualTo(0.6);
        assertThat(invoke(dify, "getSceneWorkflowId")).isEqualTo("meme-scene-recognizer");
        assertThat(invoke(dify, "getPassiveChatWorkflowId")).isEqualTo("passive-chat-reply");
        assertThat(invoke(dify, "getActiveWorkflowId")).isEqualTo("active-chat-reply");
        assertThat(invoke(dify, "getMemeSceneApiKey")).isEqualTo("test-meme-key");
        assertThat(invoke(dify, "getPassiveChatApiKey")).isEqualTo("test-passive-key");
        assertThat(invoke(dify, "getActiveChatApiKey")).isEqualTo("test-active-key");
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
