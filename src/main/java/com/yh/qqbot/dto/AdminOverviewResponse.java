package com.yh.qqbot.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record AdminOverviewResponse(
        String groupId,
        Instant generatedAt,
        Long importBatches,
        Long rawMessages,
        Long cleanMessages,
        Long sessions,
        Long memberStats,
        Long knowledgeCandidates,
        Long pendingKnowledgeCandidates,
        Long memberCandidates,
        Long pendingMemberCandidates,
        Long activeGroupKnowledge,
        Long enabledGroupKnowledge,
        Long activeMemberProfiles,
        Long enabledMemberProfiles,
        Long successfulEmbeddings,
        Long triggerLogsToday,
        Long adminOpsToday,
        LatestImport latestImport
) {

    public record LatestImport(
            Long batchId,
            String status,
            Long rawCount,
            Long cleanCount,
            Long sessionCount,
            Long memberCount,
            String sourceFile,
            LocalDateTime createdAt
    ) {
    }
}
