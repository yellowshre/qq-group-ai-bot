package com.yh.qqbot.chat.history.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ChatMemberRankRequest(
        @NotBlank String groupId,
        Long batchId,
        @NotBlank String rankType,
        LocalDate startDate,
        LocalDate endDate,
        Integer topN
) {
}
