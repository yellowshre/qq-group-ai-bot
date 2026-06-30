package com.yh.qqbot.adapter.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DevMessageControllerReflectionTest {

    @Test
    void privateSimulationReturnsIgnoredResultWithoutOutbound() throws Exception {
        Object notCommand = invokeStatic(cls("com.yh.qqbot.dto.CommandHandleResult"), "notCommand");
        Object service = privateAdminService(notCommand);
        Object controller = controller(service);
        Object request = privateRequest("10000", "private-msg-1", "#群 736566774 状态");

        Object response = invoke(controller, "privateMessage",
                new Class<?>[]{cls("com.yh.qqbot.dto.DevPrivateMessageRequest")},
                request);

        assertThat(invoke(response, "handled")).isEqualTo(false);
        assertThat(invoke(response, "shouldReply")).isEqualTo(false);
        assertThat(invoke(response, "outboundMessage")).isNull();
        assertThat(invoke(response, "operation")).isNull();
    }

    @Test
    void privateSimulationReturnsHandledResultWithReply() throws Exception {
        Object handled = invokeStatic(
                cls("com.yh.qqbot.dto.CommandHandleResult"),
                "handled",
                new Class<?>[]{String.class, String.class, String.class},
                "STATUS",
                "group status",
                "当前群配置");
        Object service = privateAdminService(handled);
        Object controller = controller(service);
        Object request = privateRequest("885391366", "private-msg-2", "#群 736566774 状态");

        Object response = invoke(controller, "privateMessage",
                new Class<?>[]{cls("com.yh.qqbot.dto.DevPrivateMessageRequest")},
                request);

        assertThat(invoke(response, "handled")).isEqualTo(true);
        assertThat(invoke(response, "shouldReply")).isEqualTo(true);
        assertThat(invoke(response, "operation")).isEqualTo("STATUS");
        assertThat(invoke(response, "detail")).isEqualTo("group status");
        assertThat(invoke(invoke(response, "outboundMessage"), "text")).isEqualTo("当前群配置");
    }

    private Object controller(Object privateAdminCommandService) throws Exception {
        Object inboundAdapter = mockClass("com.yh.qqbot.adapter.onebot.OneBotInboundAdapter");
        return cls("com.yh.qqbot.adapter.dev.DevMessageController")
                .getConstructor(
                        cls("com.yh.qqbot.adapter.onebot.OneBotInboundAdapter"),
                        cls("com.yh.qqbot.service.command.PrivateAdminCommandService"))
                .newInstance(inboundAdapter, privateAdminCommandService);
    }

    private Object privateAdminService(Object result) throws Exception {
        Class<?> serviceType = cls("com.yh.qqbot.service.command.PrivateAdminCommandService");
        Object service = Mockito.mock(serviceType);
        Object stub = Mockito.doReturn(result).when(service);
        invoke(stub, "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                Mockito.any(cls("com.yh.qqbot.dto.BotPrivateMessage")));
        return service;
    }

    private Object privateRequest(String userId, String messageId, String rawMessage) throws Exception {
        return cls("com.yh.qqbot.dto.DevPrivateMessageRequest")
                .getConstructor(String.class, String.class, String.class)
                .newInstance(userId, messageId, rawMessage);
    }

    private Object mockClass(String className) throws Exception {
        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) cls(className);
        return Mockito.mock(type);
    }

    private static Object invokeStatic(Class<?> target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getMethod(methodName, parameterTypes);
        return method.invoke(null, args);
    }

    private static Object invokeStatic(Class<?> target, String methodName) throws Exception {
        Method method = target.getMethod(methodName);
        return method.invoke(null);
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
}
