package com.yh.qqbot.dto;

public record ActiveChatPolicyResult(
        boolean allowed,
        String reason,
        String rejectReason,
        boolean randomHit,
        long cooldownSeconds,
        long maxPerHour,
        long maxPerDay) {

    public static final String ACTIVE_CHAT_DISABLED = "ACTIVE_CHAT_DISABLED";
    public static final String AT_BOT = "AT_BOT";
    public static final String BOT_NICKNAME_MATCHED = "BOT_NICKNAME_MATCHED";
    public static final String GROUP_DISABLED = "GROUP_DISABLED";
    public static final String GROUP_ACTIVE_CHAT_DISABLED = "GROUP_ACTIVE_CHAT_DISABLED";
    public static final String ADMIN_COMMAND = "ADMIN_COMMAND";
    public static final String EMPTY_MESSAGE = "EMPTY_MESSAGE";
    public static final String TOO_SHORT = "TOO_SHORT";
    public static final String TOO_LONG = "TOO_LONG";
    public static final String PUNCTUATION_ONLY = "PUNCTUATION_ONLY";
    public static final String MEME_ALREADY_SENT = "MEME_ALREADY_SENT";
    public static final String LAST_MESSAGE_FROM_BOT = "LAST_MESSAGE_FROM_BOT";
    public static final String COOLDOWN = "COOLDOWN";
    public static final String HOURLY_LIMIT = "HOURLY_LIMIT";
    public static final String DAILY_LIMIT = "DAILY_LIMIT";
    public static final String RANDOM_MISS = "RANDOM_MISS";
    public static final String ALLOWED = "ALLOWED";

    public static ActiveChatPolicyResult allowed(long cooldownSeconds, long maxPerHour) {
        return allowed(cooldownSeconds, maxPerHour, 0);
    }

    public static ActiveChatPolicyResult allowed(long cooldownSeconds, long maxPerHour, long maxPerDay) {
        return new ActiveChatPolicyResult(true, ALLOWED, ALLOWED, true, cooldownSeconds, maxPerHour, maxPerDay);
    }

    public static ActiveChatPolicyResult rejected(String rejectReason, boolean randomHit, long cooldownSeconds, long maxPerHour) {
        return rejected(rejectReason, randomHit, cooldownSeconds, maxPerHour, 0);
    }

    public static ActiveChatPolicyResult rejected(
            String rejectReason,
            boolean randomHit,
            long cooldownSeconds,
            long maxPerHour,
            long maxPerDay) {
        return new ActiveChatPolicyResult(false, rejectReason, rejectReason, randomHit, cooldownSeconds, maxPerHour, maxPerDay);
    }
}
