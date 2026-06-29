package com.yh.qqbot.chat.history.dto;

public record ChatMemberRankItem(
        int rank,
        String senderUid,
        String senderUin,
        String senderName,
        long score,
        long rawMessageCount,
        long messageCount,
        long activeDays,
        long mentionCount,
        long replyCount,
        long repliedByCount,
        long sessionCount
) {
}
