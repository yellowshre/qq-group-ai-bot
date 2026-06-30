package com.yh.qqbot.adapter.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ApiResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminApiTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-QQBOT-ADMIN-TOKEN";

    private static final Logger log = LoggerFactory.getLogger(AdminApiTokenFilter.class);

    private final QqBotProperties properties;
    private final ObjectMapper objectMapper;

    public AdminApiTokenFilter(QqBotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void logStatus() {
        QqBotProperties.AdminUi adminUi = properties.getAdminUi();
        log.info("Admin UI API token protection. enabled={}, tokenConfigured={}, protected={}",
                adminUi.isApiTokenEnabled(),
                hasText(adminUi.getApiToken()),
                protectedEnabled());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!shouldCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String expected = properties.getAdminUi().getApiToken();
        String actual = request.getHeader(HEADER_NAME);
        if (expected.equals(actual)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("UNAUTHORIZED", "Admin API token required"));
    }

    private boolean shouldCheck(HttpServletRequest request) {
        if (!protectedEnabled()) {
            return false;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/dev/");
    }

    private boolean protectedEnabled() {
        QqBotProperties.AdminUi adminUi = properties.getAdminUi();
        return adminUi.isApiTokenEnabled() && hasText(adminUi.getApiToken());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
