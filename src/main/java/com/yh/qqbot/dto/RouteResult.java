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
        Double confidence) {

    public static RouteResult silent(String reason) {
        return new RouteResult(RouteType.SILENT, RouteType.SILENT.name(), false, null, reason, true, false, false,
                null, null, null, null, null);
    }

    public static RouteResult send(RouteType routeType, OutboundMessage outboundMessage, String reason) {
        return new RouteResult(routeType, routeType.name(), true, outboundMessage, reason, true, false, false,
                null, null, null, null, null);
    }

    public RouteResult withMemeId(Long memeId) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence);
    }

    public RouteResult withDurationMs(Long durationMs) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence);
    }

    public RouteResult withDedupPassed(boolean dedupPassed) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence);
    }

    public RouteResult withAdminCommandHit(boolean adminCommandHit) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence);
    }

    public RouteResult withMemeHit(boolean memeHit) {
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, memeId, durationMs, workflowType, sceneCode, confidence);
    }

    public RouteResult withMemeMetadata(MemeMatchResult meme) {
        if (meme == null) {
            return this;
        }
        return new RouteResult(routeType, responseType, shouldSend, outboundMessage, reason, dedupPassed,
                adminCommandHit, memeHit, meme.memeId(), durationMs, meme.matchType(), meme.sceneCode(), meme.confidence());
    }
}
