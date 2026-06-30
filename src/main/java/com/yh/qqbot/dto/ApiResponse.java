package com.yh.qqbot.dto;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "ok", data, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }
}
