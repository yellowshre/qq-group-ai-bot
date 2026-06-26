package com.yh.qqbot.service.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class GroupPersonaServiceReflectionTest {

    private static final String DEFAULT_PERSONA =
            "\u4f60\u662f\u4e00\u4e2a\u8bf4\u8bdd\u7b80\u77ed\u3001\u81ea\u7136\u3001\u7565\u5e26\u5410\u69fd\u4f46\u4e0d\u6076\u610f\u653b\u51fb\u4eba\u7684 QQ \u7fa4\u673a\u5668\u4eba\u3002";

    @Test
    void returnsDefaultPersonaWhenGroupConfigUnavailable() throws Exception {
        Object service = newService(groupConfigProxy(true), newProperties());

        Object persona = invoke(service, "getPersona", new Class<?>[]{Long.class}, 10001L);

        assertThat(persona).isEqualTo(DEFAULT_PERSONA);
    }

    @Test
    void returnsDefaultPersonaWhenGroupIdIsNull() throws Exception {
        Object service = newService(groupConfigProxy(false), newProperties());

        Object persona = invoke(service, "getPersona", new Class<?>[]{Long.class}, new Object[]{null});

        assertThat(persona).isEqualTo(DEFAULT_PERSONA);
    }

    private Object newService(Object groupConfigService, Object properties) throws Exception {
        return cls("com.yh.qqbot.service.chat.GroupPersonaService")
                .getConstructor(cls("com.yh.qqbot.service.config.GroupConfigService"), cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(groupConfigService, properties);
    }

    private Object groupConfigProxy(boolean throwOnGetConfig) throws Exception {
        Class<?> serviceClass = cls("com.yh.qqbot.service.config.GroupConfigService");
        return Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                (proxy, method, args) -> {
                    if ("getConfig".equals(method.getName()) && throwOnGetConfig) {
                        throw new IllegalStateException("config unavailable");
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object newProperties() throws Exception {
        return cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
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
}
