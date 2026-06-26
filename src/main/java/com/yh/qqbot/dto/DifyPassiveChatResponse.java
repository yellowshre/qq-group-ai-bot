package com.yh.qqbot.dto;

public record DifyPassiveChatResponse(String replyText, Double confidence) {

    public boolean valid() {
        return replyText != null && !replyText.isBlank() && confidence != null;
    }

    public PassiveChatReply toPassiveChatReply() {
        return new PassiveChatReply(replyText == null ? "" : replyText.strip(), confidence == null ? 0 : confidence);
    }
}
