package com.yh.qqbot.chat.history.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryInsightResponse(
        String groupId,
        Long batchId,
        LocalDate startDate,
        LocalDate endDate,
        int topN,
        Summary summary,
        List<DailyActivity> dailyActivities,
        List<MemberDigest> topMembers
) {

    public record Summary(
            Long rawMessages,
            Long cleanMessages,
            Long mentions,
            Long replies,
            Long repliedBy,
            Long sessions,
            Long members,
            Long activeDays,
            LocalDateTime firstMessageAt,
            LocalDateTime lastMessageAt
    ) {
    }

    public record DailyActivity(
            LocalDate statDate,
            Long rawMessages,
            Long cleanMessages,
            Long mentions,
            Long replies,
            Long repliedBy,
            Long sessions,
            Long activeMembers
    ) {
    }

    public record MemberDigest(
            String senderUid,
            String senderUin,
            String senderName,
            Long rawMessages,
            Long cleanMessages,
            Long mentions,
            Long replies,
            Long repliedBy,
            Long sessions
    ) {
    }
}
