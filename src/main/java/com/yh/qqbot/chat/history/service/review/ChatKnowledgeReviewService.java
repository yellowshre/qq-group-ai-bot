package com.yh.qqbot.chat.history.service.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.CandidateReviewRequest;
import com.yh.qqbot.chat.history.dto.ChatCandidateStatus;
import com.yh.qqbot.chat.history.dto.ChatReviewTargetType;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeReviewLogEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeReviewLogMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatKnowledgeReviewService {

    private static final String DEFAULT_REVIEWER = "local-admin";

    private final ChatKnowledgeCandidateMapper knowledgeCandidateMapper;
    private final ChatMemberCandidateMapper memberCandidateMapper;
    private final ChatKnowledgeReviewLogMapper reviewLogMapper;

    public ChatKnowledgeReviewService(
            ChatKnowledgeCandidateMapper knowledgeCandidateMapper,
            ChatMemberCandidateMapper memberCandidateMapper,
            ChatKnowledgeReviewLogMapper reviewLogMapper) {
        this.knowledgeCandidateMapper = knowledgeCandidateMapper;
        this.memberCandidateMapper = memberCandidateMapper;
        this.reviewLogMapper = reviewLogMapper;
    }

    public ChatKnowledgeCandidateEntity reviewKnowledgeCandidate(Long id, CandidateReviewRequest request) {
        ChatKnowledgeCandidateEntity candidate = knowledgeCandidateMapper.selectById(id);
        if (candidate == null) {
            throw new InvalidChatCandidateRequestException("candidate not found");
        }
        String oldStatus = candidate.getStatus();
        ChatCandidateStatus newStatus = parseStatus(request.status());
        candidate.setStatus(newStatus.name());
        candidate.setReviewer(reviewer(request.reviewer()));
        candidate.setReviewComment(limit(request.reviewComment(), 500));
        candidate.setReviewedAt(LocalDateTime.now());
        knowledgeCandidateMapper.updateById(candidate);
        recordLog(ChatReviewTargetType.KNOWLEDGE_CANDIDATE, id, oldStatus, newStatus.name(), request);
        return candidate;
    }

    public ChatMemberCandidateEntity reviewMemberCandidate(Long id, CandidateReviewRequest request) {
        ChatMemberCandidateEntity candidate = memberCandidateMapper.selectById(id);
        if (candidate == null) {
            throw new InvalidChatCandidateRequestException("member candidate not found");
        }
        String oldStatus = candidate.getStatus();
        ChatCandidateStatus newStatus = parseStatus(request.status());
        candidate.setStatus(newStatus.name());
        candidate.setReviewer(reviewer(request.reviewer()));
        candidate.setReviewComment(limit(request.reviewComment(), 500));
        candidate.setReviewedAt(LocalDateTime.now());
        memberCandidateMapper.updateById(candidate);
        recordLog(ChatReviewTargetType.MEMBER_CANDIDATE, id, oldStatus, newStatus.name(), request);
        return candidate;
    }

    public List<ChatKnowledgeReviewLogEntity> findReviewLogs(String targetType, Long targetId) {
        LambdaQueryWrapper<ChatKnowledgeReviewLogEntity> wrapper = new LambdaQueryWrapper<ChatKnowledgeReviewLogEntity>()
                .orderByDesc(ChatKnowledgeReviewLogEntity::getCreatedAt);
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(ChatKnowledgeReviewLogEntity::getTargetType, parseTargetType(targetType).name());
        }
        if (targetId != null) {
            wrapper.eq(ChatKnowledgeReviewLogEntity::getTargetId, targetId);
        }
        return reviewLogMapper.selectList(wrapper);
    }

    private void recordLog(
            ChatReviewTargetType targetType,
            Long targetId,
            String oldStatus,
            String newStatus,
            CandidateReviewRequest request) {
        ChatKnowledgeReviewLogEntity log = new ChatKnowledgeReviewLogEntity();
        log.setTargetType(targetType.name());
        log.setTargetId(targetId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setReviewer(reviewer(request.reviewer()));
        log.setReviewComment(limit(request.reviewComment(), 500));
        reviewLogMapper.insert(log);
    }

    private ChatCandidateStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidChatCandidateRequestException("status is required");
        }
        try {
            return ChatCandidateStatus.valueOf(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidChatCandidateRequestException("status is invalid");
        }
    }

    private ChatReviewTargetType parseTargetType(String value) {
        try {
            return ChatReviewTargetType.valueOf(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidChatCandidateRequestException("targetType is invalid");
        }
    }

    private String reviewer(String value) {
        return value == null || value.isBlank() ? DEFAULT_REVIEWER : value.strip();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
