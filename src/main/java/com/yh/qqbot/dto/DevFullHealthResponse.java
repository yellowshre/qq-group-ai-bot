package com.yh.qqbot.dto;

import java.util.List;

public record DevFullHealthResponse(
        List<String> activeProfiles,
        DependencyStatus mysql,
        DependencyStatus redis,
        boolean difyEnabled,
        boolean memeCachePreheatEnabled,
        String messageSenderType,
        Long sceneDictCount,
        Long enabledMemeMaterialCount,
        OneBotStatus oneBot,
        DifyStatus dify,
        String memeBaseDir,
        boolean knowledgeEmbeddingEnabled,
        boolean knowledgeContextEnabled,
        boolean memeKnowledgeEnabled,
        boolean passiveChatKnowledgeEnabled,
        boolean activeChatKnowledgeEnabled,
        KnowledgeContextConfig knowledgeContextConfig,
        GroupConfigSnapshot groupConfig
) {

    public record DependencyStatus(boolean reachable, String detail) {
    }

    public record OneBotStatus(
            boolean wsEnabled,
            String selfId,
            List<String> allowedGroupIds) {
    }

    public record DifyStatus(
            boolean enabled,
            boolean baseUrlConfigured,
            boolean memeSceneWorkflowConfigured,
            boolean passiveChatWorkflowConfigured,
            boolean activeChatWorkflowConfigured,
            boolean memeSceneApiKeyConfigured,
            boolean passiveChatApiKeyConfigured,
            boolean activeChatApiKeyConfigured) {
    }

    public record KnowledgeContextConfig(
            int maxItems,
            int maxLength,
            double minScore,
            int memberProfileLimit,
            int maxSearchCandidates,
            int maxItemContentLength) {
    }
}
