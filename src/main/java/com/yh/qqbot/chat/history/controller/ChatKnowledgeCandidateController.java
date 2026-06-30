package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.CandidateReviewRequest;
import com.yh.qqbot.chat.history.dto.ChatCandidateGenerateRequest;
import com.yh.qqbot.chat.history.dto.ChatCandidateGenerateResponse;
import com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest;
import com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateResponse;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeReviewLogEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity;
import com.yh.qqbot.chat.history.service.candidate.ChatCandidateQueryService;
import com.yh.qqbot.chat.history.service.candidate.ChatKnowledgeCandidateGenerationService;
import com.yh.qqbot.chat.history.service.candidate.ManualKnowledgeCandidateService;
import com.yh.qqbot.chat.history.service.review.ChatKnowledgeReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatKnowledgeCandidateController {

    private final ChatKnowledgeCandidateGenerationService generationService;
    private final ManualKnowledgeCandidateService manualCandidateService;
    private final ChatCandidateQueryService queryService;
    private final ChatKnowledgeReviewService reviewService;

    public ChatKnowledgeCandidateController(
            ChatKnowledgeCandidateGenerationService generationService,
            ManualKnowledgeCandidateService manualCandidateService,
            ChatCandidateQueryService queryService,
            ChatKnowledgeReviewService reviewService) {
        this.generationService = generationService;
        this.manualCandidateService = manualCandidateService;
        this.queryService = queryService;
        this.reviewService = reviewService;
    }

    @PostMapping("/candidates/generate")
    public ChatCandidateGenerateResponse generateCandidates(@Valid @RequestBody ChatCandidateGenerateRequest request) {
        return generationService.generate(request.batchId(), request.groupId());
    }

    @PostMapping("/candidates/manual")
    public ManualKnowledgeCandidateResponse addManualCandidate(
            @Valid @RequestBody ManualKnowledgeCandidateRequest request) {
        return manualCandidateService.addManualCandidate(request);
    }

    @GetMapping("/candidates")
    public List<ChatKnowledgeCandidateEntity> knowledgeCandidates(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String status) {
        return queryService.findKnowledgeCandidates(batchId, groupId, status);
    }

    @PostMapping("/candidates/{id}/review")
    public ChatKnowledgeCandidateEntity reviewKnowledgeCandidate(
            @PathVariable Long id,
            @Valid @RequestBody CandidateReviewRequest request) {
        return reviewService.reviewKnowledgeCandidate(id, request);
    }

    @GetMapping("/member-candidates")
    public List<ChatMemberCandidateEntity> memberCandidates(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String status) {
        return queryService.findMemberCandidates(batchId, groupId, status);
    }

    @PostMapping("/member-candidates/{id}/review")
    public ChatMemberCandidateEntity reviewMemberCandidate(
            @PathVariable Long id,
            @Valid @RequestBody CandidateReviewRequest request) {
        return reviewService.reviewMemberCandidate(id, request);
    }

    @GetMapping("/review-logs")
    public List<ChatKnowledgeReviewLogEntity> reviewLogs(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId) {
        return reviewService.findReviewLogs(targetType, targetId);
    }
}
