package com.yh.qqbot.adapter.onebot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class OneBotWebSocketClientTest {

    @Test
    void appendsAccessTokenToConnectionUri() throws Exception {
        Object client = client("ws://127.0.0.1:3001/", "test-token");

        URI uri = (URI) invokeDeclared(client, "connectionUri");

        assertThat(uri.toString()).isEqualTo("ws://127.0.0.1:3001/?access_token=test-token");
    }

    @Test
    void appendsAccessTokenWithExistingQueryString() throws Exception {
        Object client = client("ws://127.0.0.1:3001/ws?client=qqbot", "test-token");

        URI uri = (URI) invokeDeclared(client, "connectionUri");

        assertThat(uri.toString()).isEqualTo("ws://127.0.0.1:3001/ws?client=qqbot&access_token=test-token");
    }

    private Object client(String url, String token) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object onebot = invoke(properties, "getOnebot");
        Object ws = invoke(onebot, "getWs");
        ws.getClass().getMethod("setUrl", String.class).invoke(ws, url);
        ws.getClass().getMethod("setAccessToken", String.class).invoke(ws, token);
        return cls("com.yh.qqbot.adapter.onebot.OneBotWebSocketClient")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"),
                        ObjectMapper.class, ObjectProvider.class, Executor.class)
                .newInstance(properties, new ObjectMapper(), objectProvider(), (Executor) Runnable::run);
    }

    private Object objectProvider() {
        return Proxy.newProxyInstance(
                ObjectProvider.class.getClassLoader(),
                new Class<?>[]{ObjectProvider.class},
                (proxy, method, args) -> null
        );
    }

    private Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private Object invokeDeclared(Object target, String methodName) throws Exception {
        var method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
