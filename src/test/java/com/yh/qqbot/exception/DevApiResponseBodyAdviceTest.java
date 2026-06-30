package com.yh.qqbot.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DevApiResponseBodyAdviceTest {

    @Test
    void wrapsDevJsonSuccessBody() {
        Map<String, Object> body = Map.of("value", 1);

        Object result = write("/dev/health/full", body, 200);

        assertThat(result.getClass().getName()).isEqualTo("com.yh.qqbot.dto.ApiResponse");
        assertThat(invoke(result, "success")).isEqualTo(true);
        assertThat(invoke(result, "code")).isEqualTo("OK");
        assertThat(invoke(result, "data")).isSameAs(body);
    }

    @Test
    void skipsNonDevPath() {
        Map<String, Object> body = Map.of("value", 1);

        Object result = write("/admin/", body, 200);

        assertThat(result).isSameAs(body);
    }

    @Test
    void skipsErrorStatus() {
        Map<String, Object> body = Map.of("value", 1);

        Object result = write("/dev/health/full", body, 400);

        assertThat(result).isSameAs(body);
    }

    private Object write(String path, Object body, int status) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", path);
        servletRequest.setRequestURI(path);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        servletResponse.setStatus(status);
        try {
            Object advice = cls("com.yh.qqbot.exception.DevApiResponseBodyAdvice")
                    .getConstructor()
                    .newInstance();
            Method method = advice.getClass().getMethod(
                    "beforeBodyWrite",
                    Object.class,
                    MethodParameter.class,
                    MediaType.class,
                    Class.class,
                    ServerHttpRequest.class,
                    ServerHttpResponse.class);
            return method.invoke(
                    advice,
                    body,
                    null,
                    MediaType.APPLICATION_JSON,
                    null,
                    new ServletServerHttpRequest(servletRequest),
                    new ServletServerHttpResponse(servletResponse));
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
