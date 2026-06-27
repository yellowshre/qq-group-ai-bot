package com.yh.qqbot.dto;

public record ActiveChatReplyResult(
        boolean shouldReply,
        String replyText,
        double confidence,
        String workflowType,
        String rejectReason,
        boolean success) {

    public static final String WORKFLOW_TYPE = "ACTIVE_DIFY_CHAT";
    public static final String DIFY_DISABLED = "DIFY_DISABLED";
    public static final String API_KEY_MISSING = "API_KEY_MISSING";
    public static final String DIFY_ERROR = "DIFY_ERROR";
    public static final String EMPTY_REPLY = "EMPTY_REPLY";
    public static final String SHOULD_REPLY_FALSE = "SHOULD_REPLY_FALSE";
    public static final String LOW_CONFIDENCE = "LOW_CONFIDENCE";
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";
    public static final String NONE = "NONE";

    public static ActiveChatReplyResult success(String replyText, double confidence) {
        return new ActiveChatReplyResult(true, replyText, confidence, WORKFLOW_TYPE, NONE, true);
    }

    public static ActiveChatReplyResult rejected(String rejectReason) {
        return rejected(rejectReason, "", 0);
    }

    public static ActiveChatReplyResult rejected(String rejectReason, String replyText, double confidence) {
        return new ActiveChatReplyResult(false, replyText, confidence, WORKFLOW_TYPE, rejectReason, false);
    }
}
