package com.yh.qqbot.exception;

import com.yh.qqbot.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class DevApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!shouldWrap(body, selectedContentType, request, response)) {
            return body;
        }
        return ApiResponse.ok(body);
    }

    private boolean shouldWrap(
            Object body,
            MediaType selectedContentType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (path == null || !path.startsWith("/dev/")) {
            return false;
        }
        if (!jsonLike(selectedContentType)) {
            return false;
        }
        if (body instanceof ApiResponse<?>
                || body instanceof String
                || body instanceof byte[]
                || body instanceof Resource) {
            return false;
        }
        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();
            return status >= 200 && status < 300 && status != 204;
        }
        return true;
    }

    private boolean jsonLike(MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || mediaType.getSubtype().endsWith("+json");
    }
}
