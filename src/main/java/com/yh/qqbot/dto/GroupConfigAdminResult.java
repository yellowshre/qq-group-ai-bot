package com.yh.qqbot.dto;

public record GroupConfigAdminResult(
        String operation,
        String detail,
        String replyText
) {
}
