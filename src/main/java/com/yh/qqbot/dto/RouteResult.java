package com.yh.qqbot.dto;

import com.yh.qqbot.enums.RouteType;

public record RouteResult(
        RouteType routeType,
        String responseType,
        boolean shouldSend,
        OutboundMessage outboundMessage,
        String reason,
        boolean dedupPassed,
        boolean adminCommandHit,
        boolean memeHit,
        Long memeId,
        Long durationMs,
        String workflowType,
        String sceneCode,
        Double confidence,
        boolean passiveChatHit,
        String replyText,
        Double chatConfidence,
        String silentReason,
        Boolean activeChatHit,
        Boolean activeShouldReply,
        Double activeConfidence,
        String activeReason,
        Boolean activePolicyPassed,
        String activePolicyRejectReason,
        Long cooldownSeconds,
        Long hourCount,
        Boolean randomHit) {

    public static RouteResult silent(String reason) {
        return new RouteResult(RouteType.SILENT, RouteType.SILENT.name(), false, null, reason, true, false, false,
                null, null, null, null, null, false, null, null, reason,
                false, null, null, null, null, null, null, null, null);
    }

    public static RouteResult send(RouteType routeType, OutboundMessage outboundMessage, String reason) {
        return new RouteResult(routeType, routeType.name(), true, outboundMessage, reason, true, false, false,
                null, null, null, null, null, false, outboundMessage == null ? null : outboundMessage.text(), null, null,
                false, null, null, null, null, null, null, null, null);
    }

    public RouteResult withMemeId(Long memeId) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withDurationMs(Long durationMs) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withDedupPassed(boolean dedupPassed) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withAdminCommandHit(boolean adminCommandHit) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withMemeHit(boolean memeHit) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withMemeMetadata(MemeMatchResult meme) {
        if (meme == null) {
            return this;
        }
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, meme.memeId(), durationMs, meme.matchType(), meme.sceneCode(), meme.confidence(),
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withPassiveChatHit(boolean passiveChatHit) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withReplyText(String replyText) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withChatConfidence(Double chatConfidence) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withWorkflowType(String workflowType) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withSilentReason(String silentReason) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActiveChatHit(Boolean activeChatHit) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActiveShouldReply(Boolean activeShouldReply) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActiveConfidence(Double activeConfidence) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActiveReason(String activeReason) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActivePolicyPassed(Boolean activePolicyPassed) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActivePolicyRejectReason(String activePolicyRejectReason) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withCooldownSeconds(Long cooldownSeconds) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withHourCount(Long hourCount) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withRandomHit(Boolean randomHit) {
        return copy(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }

    public RouteResult withActivePolicy(ActiveChatPolicyResult policy) {
        if (policy == null) {
            return this;
        }
        return withActivePolicyPassed(policy.allowed())
                .withActiveReason(policy.reason())
                .withActivePolicyRejectReason(policy.allowed() ? null : policy.rejectReason())
                .withCooldownSeconds(policy.cooldownSeconds())
                .withRandomHit(policy.randomHit());
    }

    public RouteResult withActiveReply(ActiveChatReplyResult reply) {
        if (reply == null) {
            return this;
        }
        return withActiveShouldReply(reply.shouldReply())
                .withActiveConfidence(reply.confidence())
                .withActivePolicyRejectReason(reply.success() ? activePolicyRejectReason : reply.rejectReason())
                .withWorkflowType(reply.workflowType())
                .withReplyText(reply.replyText());
    }

    private RouteResult copy(
            RouteType routeType,
            String responseType,
            boolean shouldSend,
            OutboundMessage outboundMessage,
            String reason,
            boolean dedupPassed,
            boolean adminCommandHit,
            boolean memeHit,
            Long memeId,
            Long durationMs,
            String workflowType,
            String sceneCode,
            Double confidence,
            boolean passiveChatHit,
            String replyText,
            Double chatConfidence,
            String silentReason,
            Boolean activeChatHit,
            Boolean activeShouldReply,
            Double activeConfidence,
            String activeReason,
            Boolean activePolicyPassed,
            String activePolicyRejectReason,
            Long cooldownSeconds,
            Long hourCount,
            Boolean randomHit) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence,
                passiveChatHit, replyText, chatConfidence, silentReason,
                activeChatHit, activeShouldReply, activeConfidence, activeReason, activePolicyPassed,
                activePolicyRejectReason, cooldownSeconds, hourCount, randomHit);
    }
}
