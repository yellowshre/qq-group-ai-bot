package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest;
import com.yh.qqbot.chat.history.dto.FormalKnowledgePublishResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeStatusChangeRequest;
import com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberProfileEntity;
import com.yh.qqbot.chat.history.service.formal.FormalKnowledgeService;
import com.yh.qqbot.chat.history.service.vector.KnowledgeEmbeddingService;
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
public class ChatFormalKnowledgeController {

    private final FormalKnowledgeService formalKnowledgeService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    public ChatFormalKnowledgeController(
            FormalKnowledgeService formalKnowledgeService,
            KnowledgeEmbeddingService knowledgeEmbeddingService) {
        this.formalKnowledgeService = formalKnowledgeService;
        this.knowledgeEmbeddingService = knowledgeEmbeddingService;
    }

    @PostMapping("/knowledge/publish")
    public FormalKnowledgePublishResponse publishKnowledge(
            @Valid @RequestBody FormalKnowledgePublishRequest request) {
        return formalKnowledgeService.publishKnowledge(request);
    }

    @PostMapping("/member-profiles/publish")
    public FormalKnowledgePublishResponse publishMemberProfiles(
            @Valid @RequestBody FormalKnowledgePublishRequest request) {
        return formalKnowledgeService.publishMemberProfiles(request);
    }

    @GetMapping("/knowledge")
    public List<ChatGroupKnowledgeEntity> knowledge(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) Boolean enabled) {
        return formalKnowledgeService.findKnowledge(groupId, enabled);
    }

    @GetMapping("/member-profiles")
    public List<ChatMemberProfileEntity> memberProfiles(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) Boolean enabled) {
        return formalKnowledgeService.findMemberProfiles(groupId, enabled);
    }

    @PostMapping("/knowledge/{id}/disable")
    public ChatGroupKnowledgeEntity disableKnowledge(
            @PathVariable Long id,
            @RequestBody(required = false) KnowledgeStatusChangeRequest request) {
        return formalKnowledgeService.setKnowledgeEnabled(id, false, operator(request), comment(request));
    }

    @PostMapping("/knowledge/{id}/enable")
    public ChatGroupKnowledgeEntity enableKnowledge(
            @PathVariable Long id,
            @RequestBody(required = false) KnowledgeStatusChangeRequest request) {
        return formalKnowledgeService.setKnowledgeEnabled(id, true, operator(request), comment(request));
    }

    @PostMapping("/member-profiles/{id}/disable")
    public ChatMemberProfileEntity disableMemberProfile(
            @PathVariable Long id,
            @RequestBody(required = false) KnowledgeStatusChangeRequest request) {
        return formalKnowledgeService.setMemberProfileEnabled(id, false, operator(request), comment(request));
    }

    @PostMapping("/member-profiles/{id}/enable")
    public ChatMemberProfileEntity enableMemberProfile(
            @PathVariable Long id,
            @RequestBody(required = false) KnowledgeStatusChangeRequest request) {
        return formalKnowledgeService.setMemberProfileEnabled(id, true, operator(request), comment(request));
    }

    @PostMapping("/knowledge/embeddings/generate")
    public KnowledgeEmbeddingGenerateResponse generateEmbeddings(
            @Valid @RequestBody KnowledgeEmbeddingGenerateRequest request) {
        return knowledgeEmbeddingService.generate(request);
    }

    @PostMapping("/knowledge/search")
    public KnowledgeSearchResponse searchKnowledge(
            @Valid @RequestBody KnowledgeSearchRequest request) {
        return knowledgeEmbeddingService.search(request);
    }

    private String operator(KnowledgeStatusChangeRequest request) {
        return request == null ? null : request.operator();
    }

    private String comment(KnowledgeStatusChangeRequest request) {
        return request == null ? null : request.comment();
    }
}
