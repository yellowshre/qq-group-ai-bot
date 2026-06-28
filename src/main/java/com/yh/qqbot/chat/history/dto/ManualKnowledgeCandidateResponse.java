package com.yh.qqbot.chat.history.dto;

import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;

public record ManualKnowledgeCandidateResponse(
        ChatKnowledgeCandidateEntity candidate,
        boolean duplicate
) {
}
