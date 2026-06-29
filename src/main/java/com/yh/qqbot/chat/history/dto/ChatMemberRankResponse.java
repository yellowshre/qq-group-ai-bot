package com.yh.qqbot.chat.history.dto;

import java.time.LocalDate;
import java.util.List;

public record ChatMemberRankResponse(
        String groupId,
        Long batchId,
        String rankType,
        String rankTypeLabel,
        LocalDate startDate,
        LocalDate endDate,
        int topN,
        List<ChatMemberRankItem> items
) {
}
