package com.yh.qqbot.chat.history.service.candidate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatCandidateStatus;
import com.yh.qqbot.chat.history.dto.ChatKnowledgeCandidateType;
import com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateRequest;
import com.yh.qqbot.chat.history.dto.ManualKnowledgeCandidateResponse;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ManualKnowledgeCandidateService {

    private static final String DEFAULT_REVIEWER = "local-admin";
    private static final List<String> DUPLICATE_STATUSES = List.of(
            ChatCandidateStatus.PENDING.name(),
            ChatCandidateStatus.APPROVED.name()
    );

    private final ChatKnowledgeCandidateMapper knowledgeCandidateMapper;

    public ManualKnowledgeCandidateService(ChatKnowledgeCandidateMapper knowledgeCandidateMapper) {
        this.knowledgeCandidateMapper = knowledgeCandidateMapper;
    }

    public ManualKnowledgeCandidateResponse addManualCandidate(ManualKnowledgeCandidateRequest request) {
        ChatKnowledgeCandidateType type = parseCandidateType(request.candidateType());
        String title = requireLength(request.title(), "title", 1, 50);
        String content = requireLength(request.content(), "content", 1, 500);
        String evidenceText = optionalLimit(request.evidenceText(), 500);
        String reviewer = optionalDefault(request.reviewer(), DEFAULT_REVIEWER);

        ChatKnowledgeCandidateEntity existing = knowledgeCandidateMapper.selectOne(
                new LambdaQueryWrapper<ChatKnowledgeCandidateEntity>()
                        .eq(ChatKnowledgeCandidateEntity::getBatchId, request.batchId())
                        .eq(ChatKnowledgeCandidateEntity::getGroupId, request.groupId())
                        .eq(ChatKnowledgeCandidateEntity::getCandidateType, type.name())
                        .eq(ChatKnowledgeCandidateEntity::getContent, content)
                        .in(ChatKnowledgeCandidateEntity::getStatus, DUPLICATE_STATUSES)
                        .last("LIMIT 1"));
        if (existing != null) {
            return new ManualKnowledgeCandidateResponse(existing, true);
        }

        ChatKnowledgeCandidateEntity candidate = new ChatKnowledgeCandidateEntity();
        candidate.setBatchId(request.batchId());
        candidate.setGroupId(request.groupId());
        candidate.setCandidateType(type.name());
        candidate.setTitle(title);
        candidate.setContent(content);
        candidate.setEvidenceText(evidenceText);
        candidate.setHitCount(1L);
        candidate.setMemberCount(1L);
        candidate.setConfidence(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        candidate.setStatus(ChatCandidateStatus.PENDING.name());
        candidate.setReviewer(reviewer);
        candidate.setReviewComment(optionalLimit(request.reviewComment(), 500));
        knowledgeCandidateMapper.insert(candidate);
        return new ManualKnowledgeCandidateResponse(candidate, false);
    }

    private ChatKnowledgeCandidateType parseCandidateType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidChatCandidateRequestException("candidateType is required");
        }
        try {
            return ChatKnowledgeCandidateType.valueOf(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidChatCandidateRequestException("candidateType is invalid");
        }
    }

    private String requireLength(String value, String fieldName, int minLength, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidChatCandidateRequestException(fieldName + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new InvalidChatCandidateRequestException(fieldName + " length is invalid");
        }
        return normalized;
    }

    private String optionalLimit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String optionalDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }
}
