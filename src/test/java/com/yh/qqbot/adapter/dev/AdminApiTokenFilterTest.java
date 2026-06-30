package com.yh.qqbot.adapter.dev;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminApiTokenFilterTest {

    @Test
    void disabledProtectionAllowsDevRequestWithoutToken() throws Exception {
        Filter filter = filter(properties(false, "secret"));
        MockHttpServletRequest request = request("/dev/health/full");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void enabledProtectionRejectsMissingToken() throws Exception {
        Filter filter = filter(properties(true, "secret"));
        MockHttpServletRequest request = request("/dev/admin/groups");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Admin API token required");
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @Test
    void enabledProtectionAllowsMatchingToken() throws Exception {
        Filter filter = filter(properties(true, "secret"));
        MockHttpServletRequest request = request("/dev/admin/groups");
        request.addHeader("X-QQBOT-ADMIN-TOKEN", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void nonDevPathBypassesProtection() throws Exception {
        Filter filter = filter(properties(true, "secret"));
        MockHttpServletRequest request = request("/admin/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private static Object properties(boolean enabled, String token) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object adminUi = invoke(properties, "getAdminUi");
        adminUi.getClass().getMethod("setApiTokenEnabled", boolean.class).invoke(adminUi, enabled);
        adminUi.getClass().getMethod("setApiToken", String.class).invoke(adminUi, token);
        return properties;
    }

    private static Filter filter(Object properties) throws Exception {
        Object objectMapper = cls("com.fasterxml.jackson.databind.ObjectMapper").getConstructor().newInstance();
        objectMapper.getClass().getMethod("findAndRegisterModules").invoke(objectMapper);
        return (Filter) cls("com.yh.qqbot.adapter.dev.AdminApiTokenFilter")
                .getConstructor(
                        cls("com.yh.qqbot.config.properties.QqBotProperties"),
                        cls("com.fasterxml.jackson.databind.ObjectMapper"))
                .newInstance(properties, objectMapper);
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
