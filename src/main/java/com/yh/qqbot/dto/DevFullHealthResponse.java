package com.yh.qqbot.dto;

import java.util.List;

public record DevFullHealthResponse(
        List<String> activeProfiles,
        DependencyStatus mysql,
        DependencyStatus redis,
        boolean difyEnabled,
        boolean memeCachePreheatEnabled,
        String messageSenderType,
        AdminUiStatus adminUi,
        AdminAccessStatus adminAccess,
        BotIdentityStatus botIdentity,
        CommandAliasStatus commandAliases,
        PrivateAdminStatus privateAdmin,
        MemberRankCommandStatus memberRankCommand,
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

    public record AdminUiStatus(
            boolean apiTokenEnabled,
            boolean apiTokenConfigured,
            boolean apiTokenProtected) {
    }

    public record AdminAccessStatus(
            boolean adminsConfigured,
            int adminCount) {
    }

    public record BotIdentityStatus(
            String displayName,
            List<String> aliases,
            boolean defaultPersonaConfigured) {
    }

    public record CommandAliasStatus(
            List<String> activeChatOffWords,
            List<String> activeChatOnWords,
            List<String> extraActiveChatOffWords,
            List<String> extraActiveChatOnWords) {
    }

    public record PrivateAdminStatus(
            boolean enabled,
            boolean limitToAllowedGroups,
            String commandPrefix,
            PrivateAdminReplies replies) {
    }

    public record PrivateAdminReplies(
            String disabled,
            String groupNotAllowed,
            String unknownCommand,
            String success,
            String statusPrefix) {
    }

    public record MemberRankCommandStatus(
            boolean enabled,
            boolean groupCommandEnabled,
            boolean privateCommandEnabled,
            boolean adminOnly,
            int defaultTopN,
            int maxTopN,
            String commandPrefix) {
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
