package com.yh.qqbot.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DifyWorkflowServiceActiveReplyReflectionTest {

    @Test
    void generateActiveReplyReturnsDifyDisabledWithoutCallingClient() throws Exception {
        Object client = difyClientMock();
        Object service = service(client, properties(false, "test-active-key"));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "success")).isEqualTo(false);
        assertThat(invoke(result, "rejectReason")).isEqualTo("DIFY_DISABLED");
        verifyNoInteractions(client);
    }

    @Test
    void generateActiveReplyReturnsApiKeyMissingWithoutCallingClient() throws Exception {
        Object client = difyClientMock();
        Object service = service(client, properties(true, ""));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "success")).isEqualTo(false);
        assertThat(invoke(result, "rejectReason")).isEqualTo("API_KEY_MISSING");
        verifyNoInteractions(client);
    }

    @Test
    void generateActiveReplyRejectsWhenDifySaysShouldReplyFalse() throws Exception {
        Object service = serviceWithResponse(Map.of(
                "shouldReply", false,
                "replyText", "先不插话",
                "confidence", 0.9
        ));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "rejectReason")).isEqualTo("SHOULD_REPLY_FALSE");
    }

    @Test
    void generateActiveReplyRejectsEmptyReply() throws Exception {
        Object service = serviceWithResponse(Map.of(
                "shouldReply", true,
                "replyText", "",
                "confidence", 0.9
        ));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "rejectReason")).isEqualTo("EMPTY_REPLY");
    }

    @Test
    void generateActiveReplyRejectsLowConfidence() throws Exception {
        Object service = serviceWithResponse(Map.of(
                "shouldReply", true,
                "replyText", "这句可以接一下",
                "confidence", 0.2
        ));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "rejectReason")).isEqualTo("LOW_CONFIDENCE");
        assertThat(invoke(result, "replyText")).isEqualTo("这句可以接一下");
        assertThat(invoke(result, "confidence")).isEqualTo(0.2);
    }

    @Test
    void generateActiveReplyReturnsSuccessAndSendsStringIdsToDifyClient() throws Exception {
        Object client = difyClientMock();
        stubRunWorkflow(client, Optional.of(difyResponse(Map.of(
                "shouldReply", true,
                "replyText", "这话题我得插一句",
                "confidence", 0.9
        ))));
        Object service = service(client, properties(true, "test-active-key"));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "success")).isEqualTo(true);
        assertThat(invoke(result, "workflowType")).isEqualTo("ACTIVE_DIFY_CHAT");
        assertThat(invoke(result, "rejectReason")).isEqualTo("NONE");
        assertThat(invoke(result, "replyText")).isEqualTo("这话题我得插一句");
        assertThat(invoke(result, "confidence")).isEqualTo(0.9);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> inputsCaptor = ArgumentCaptor.forClass(Map.class);
        Object verified = Mockito.verify(client);
        runWorkflowMethod(client).invoke(verified,
                org.mockito.ArgumentMatchers.eq("active-chat-reply"),
                inputsCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("20001"),
                org.mockito.ArgumentMatchers.eq("test-active-key"));
        assertThat(inputsCaptor.getValue().get("groupId")).isInstanceOf(String.class).isEqualTo("10001");
        assertThat(inputsCaptor.getValue().get("userId")).isInstanceOf(String.class).isEqualTo("20001");
        assertThat(inputsCaptor.getValue().get("recentMessages")).isEqualTo("用户20001：这事有点意思");
    }

    @Test
    void generateActiveReplyReturnsDifyErrorWhenClientThrows() throws Exception {
        Object client = difyClientMock();
        Method method = runWorkflowMethod(client);
        when((Optional<?>) method.invoke(client,
                org.mockito.ArgumentMatchers.eq("active-chat-reply"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq("20001"),
                org.mockito.ArgumentMatchers.eq("test-active-key")))
                .thenThrow(new RuntimeException("timeout"));
        Object service = service(client, properties(true, "test-active-key"));

        Object result = generateActiveReply(service, request());

        assertThat(invoke(result, "success")).isEqualTo(false);
        assertThat(invoke(result, "rejectReason")).isEqualTo("DIFY_ERROR");
    }

    private Object serviceWithResponse(Map<String, Object> outputs) throws Exception {
        Object client = difyClientMock();
        stubRunWorkflow(client, Optional.of(difyResponse(outputs)));
        return service(client, properties(true, "test-active-key"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubRunWorkflow(Object client, Optional<?> response) throws Exception {
        Method method = runWorkflowMethod(client);
        when((Optional) method.invoke(client,
                org.mockito.ArgumentMatchers.eq("active-chat-reply"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.eq("20001"),
                org.mockito.ArgumentMatchers.eq("test-active-key")))
                .thenReturn((Optional) response);
    }

    private Method runWorkflowMethod(Object client) throws Exception {
        return client.getClass().getMethod("runWorkflow", String.class, Map.class, String.class, String.class);
    }

    private Object generateActiveReply(Object service, Object request) throws Exception {
        return invoke(service, "generateActiveReply", new Class<?>[]{cls("com.yh.qqbot.dto.ActiveChatRequest")}, request);
    }

    private Object request() throws Exception {
        return cls("com.yh.qqbot.dto.ActiveChatRequest")
                .getConstructor(String.class, Long.class, Long.class, String.class, String.class, String.class, String.class, String.class)
                .newInstance(
                        "当前群消息",
                        10001L,
                        20001L,
                        "小黄",
                        "默认人设",
                        "用户20001：这事有点意思",
                        "policy allowed",
                        "无明显风险");
    }

    private Object properties(boolean enabled, String activeChatApiKey) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object dify = invoke(properties, "getDify");
        invoke(dify, "setEnabled", new Class<?>[]{boolean.class}, enabled);
        invoke(dify, "setActiveChatApiKey", new Class<?>[]{String.class}, activeChatApiKey);
        Object workflow = invoke(dify, "getWorkflow");
        invoke(workflow, "setActiveChat", new Class<?>[]{String.class}, "active-chat-reply");
        Object activeChat = invoke(properties, "getActiveChat");
        invoke(activeChat, "setMinConfidence", new Class<?>[]{double.class}, 0.6);
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

    private Map<String, Object> difyResponse(Map<String, Object> outputs) {
        return Map.of("data", Map.of("outputs", outputs));
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
