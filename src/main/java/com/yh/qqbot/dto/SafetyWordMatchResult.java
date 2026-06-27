package com.yh.qqbot.dto;

public record SafetyWordMatchResult(
        boolean matched,
        String action,
        String matchedWord,
        boolean adminOnly) {

    public static final String ACTIVE_CHAT_ON = "ACTIVE_CHAT_ON";
    public static final String ACTIVE_CHAT_OFF = "ACTIVE_CHAT_OFF";
    public static final String NONE = "NONE";

    public static SafetyWordMatchResult none(boolean adminOnly) {
        return new SafetyWordMatchResult(false, NONE, null, adminOnly);
    }
}
