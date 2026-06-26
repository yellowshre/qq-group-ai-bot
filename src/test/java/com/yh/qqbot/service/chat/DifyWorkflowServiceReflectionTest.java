package com.yh.qqbot.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DifyWorkflowServiceReflectionTest {

    @Test
    void passiveChatWorkflowConfigIsReadable() throws Exception {
        Object properties = properties(true, "passive-chat-reply");
        Object dify = invoke(properties, "getDify");

        assertThat(invoke(dify, "getPassiveChatWorkflowId")).isEqualTo("passive-chat-reply");
    }

    @Test
    void generatePassiveReplyReturnsEmptyWhenDifyDisabled() throws Exception {
        Object service = service(null, properties(false, "passive-chat-reply"));

        Optional<?> result = (Optional<?>) invoke(service, "generatePassiveReply", passiveReplyTypes(),
                "hello", 10001L, 20001L, "bot", "persona", List.of("before"));

        assertThat(result).isEmpty();
    }

    @Test
    void generatePassiveReplyReturnsReplyWhenDifyResponseIsValid() throws Exception {
        Object client = difyClientMock();
        Object properties = properties(true, "passive-chat-reply");
        Map<String, Object> inputs = passiveInputs("hello", 10001L, 20001L, "bot", "persona", List.of("before"));
        assertThat(inputs.get("groupId")).isInstanceOf(String.class).isEqualTo("10001");
        assertThat(inputs.get("userId")).isInstanceOf(String.class).isEqualTo("20001");
        assertThat(inputs.get("recentMessages")).isInstanceOf(String.class).isEqualTo("before");
        stubRunWorkflow(client, "passive-chat-reply", inputs, "20001",
                Optional.of(difyResponse(Map.of("replyText", "hi there", "confidence", 0.88))));
        Object service = service(client, properties);

        Optional<?> result = (Optional<?>) invoke(service, "generatePassiveReply", passiveReplyTypes(),
                "hello", 10001L, 20001L, "bot", "persona", List.of("before"));

        assertThat(result).isPresent();
        Object reply = result.get();
        assertThat(invoke(reply, "replyText")).isEqualTo("hi there");
        assertThat(invoke(reply, "confidence")).isEqualTo(0.88);
    }

    @Test
    void generatePassiveReplyReturnsEmptyWhenResponseIsMalformed() throws Exception {
        Object client = difyClientMock();
        Object properties = properties(true, "passive-chat-reply");
        Map<String, Object> inputs = passiveInputs("hello", 10001L, 20001L, "bot", "persona", List.of());
        stubRunWorkflow(client, "passive-chat-reply", inputs, "20001",
                Optional.of(difyResponse(Map.of("replyText", "", "confidence", 0.88))));
        Object service = service(client, properties);

        Optional<?> result = (Optional<?>) invoke(service, "generatePassiveReply", passiveReplyTypes(),
                "hello", 10001L, 20001L, "bot", "persona", List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void recognizeMemeSceneSendsStringIds() throws Exception {
        Object client = difyClientMock();
        Object properties = properties(true, "passive-chat-reply");
        Map<String, Object> inputs = sceneInputs("funny", 10001L, 20001L);
        assertThat(inputs.get("groupId")).isInstanceOf(String.class).isEqualTo("10001");
        assertThat(inputs.get("userId")).isInstanceOf(String.class).isEqualTo("20001");
        stubRunWorkflow(client, "meme-scene", inputs, "20001",
                Optional.of(difyResponse(Map.of("sceneCode", "laugh", "confidence", 0.88))));
        Object service = service(client, properties);

        Optional<?> result = (Optional<?>) invoke(service, "recognizeMemeScene",
                new Class<?>[]{String.class, Long.class, Long.class},
                "funny", 10001L, 20001L);

        assertThat(result).isPresent();
        Object decision = result.get();
        assertThat(invoke(decision, "sceneCode")).isEqualTo("laugh");
        assertThat(invoke(decision, "confidence")).isEqualTo(0.88);
    }

    @Test
    void generatePassiveReplyReturnsEmptyWhenDifyClientThrows() throws Exception {
        Object client = difyClientMock();
        Object properties = properties(true, "passive-chat-reply");
        Map<String, Object> inputs = passiveInputs("hello", 10001L, 20001L, "bot", "persona", List.of());
        stubRunWorkflowException(client, "passive-chat-reply", inputs, "20001");
        Object service = service(client, properties);

        Optional<?> result = (Optional<?>) invoke(service, "generatePassiveReply", passiveReplyTypes(),
                "hello", 10001L, 20001L, "bot", "persona", List.of());

        assertThat(result).isEmpty();
    }

    private Object properties(boolean enabled, String passiveChatWorkflowId) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object dify = invoke(properties, "getDify");
        invoke(dify, "setEnabled", new Class<?>[]{boolean.class}, enabled);
        Object workflow = invoke(dify, "getWorkflow");
        invoke(workflow, "setPassiveChat", new Class<?>[]{String.class}, passiveChatWorkflowId);
        invoke(workflow, "setMemeScene", new Class<?>[]{String.class}, "meme-scene");
        return properties;
    }

    private Object service(Object difyClient, Object properties) throws Exception {
        return cls("com.yh.qqbot.service.chat.DifyWorkflowService")
                .getConstructor(cls("com.yh.qqbot.adapter.dify.DifyClient"), cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(difyClient, properties);
    }

    private Object difyClientMock() throws Exception {
        @SuppressWarnings("unchecked")
        Class<Object> clientClass = (Class<Object>) cls("com.yh.qqbot.adapter.dify.DifyClient");
        return Mockito.mock(clientClass);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubRunWorkflow(Object client, String workflowId, Map<String, Object> inputs, String userId, Optional<?> response)
            throws Exception {
        Method method = client.getClass().getMethod("runWorkflow", String.class, Map.class, String.class);
        when((Optional) method.invoke(client, workflowId, inputs, userId)).thenReturn((Optional) response);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubRunWorkflowException(Object client, String workflowId, Map<String, Object> inputs, String userId)
            throws Exception {
        Method method = client.getClass().getMethod("runWorkflow", String.class, Map.class, String.class);
        when((Optional) method.invoke(client, workflowId, inputs, userId)).thenThrow(new RuntimeException("timeout"));
    }

    private Map<String, Object> passiveInputs(
            String text,
            Long groupId,
            Long userId,
            String botName,
            String persona,
            List<String> recentMessages) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text);
        inputs.put("groupId", String.valueOf(groupId));
        inputs.put("userId", String.valueOf(userId));
        inputs.put("botName", botName);
        inputs.put("persona", persona);
        inputs.put("recentMessages", String.join("\n", recentMessages));
        return inputs;
    }

    private Map<String, Object> sceneInputs(String text, Long groupId, Long userId) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", text);
        inputs.put("groupId", String.valueOf(groupId));
        inputs.put("userId", String.valueOf(userId));
        return inputs;
    }

    private Map<String, Object> difyResponse(Map<String, Object> outputs) {
        return Map.of("data", Map.of("outputs", outputs));
    }

    private Class<?>[] passiveReplyTypes() {
        return new Class<?>[]{String.class, Long.class, Long.class, String.class, String.class, List.class};
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
