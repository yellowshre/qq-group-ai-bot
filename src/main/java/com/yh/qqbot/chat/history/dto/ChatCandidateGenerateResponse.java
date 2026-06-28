package com.yh.qqbot.chat.history.dto;

public record ChatCandidateGenerateResponse(
        Long batchId,
        long knowledgeCandidates,
        long memberCandidates,
        String status
) {
}
