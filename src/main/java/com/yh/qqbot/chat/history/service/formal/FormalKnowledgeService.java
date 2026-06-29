package com.yh.qqbot.chat.history.service.formal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatCandidateStatus;
import com.yh.qqbot.chat.history.dto.FormalKnowledgePublishRequest;
import com.yh.qqbot.chat.history.dto.FormalKnowledgePublishResponse;
import com.yh.qqbot.chat.history.dto.FormalKnowledgeStatus;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingTargetType;
import com.yh.qqbot.chat.history.dto.KnowledgePublishAction;
import com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgePublishLogEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberProfileEntity;
import com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgePublishLogMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormalKnowledgeService {

    private static final String DEFAULT_OPERATOR = "local-admin";

    private final ChatKnowledgeCandidateMapper knowledgeCandidateMapper;
    private final ChatMemberCandidateMapper memberCandidateMapper;
    private final ChatGroupKnowledgeMapper groupKnowledgeMapper;
    private final ChatMemberProfileMapper memberProfileMapper;
    private final ChatKnowledgePublishLogMapper publishLogMapper;

    public FormalKnowledgeService(
            ChatKnowledgeCandidateMapper knowledgeCandidateMapper,
            ChatMemberCandidateMapper memberCandidateMapper,
            ChatGroupKnowledgeMapper groupKnowledgeMapper,
            ChatMemberProfileMapper memberProfileMapper,
            ChatKnowledgePublishLogMapper publishLogMapper) {
        this.knowledgeCandidateMapper = knowledgeCandidateMapper;
        this.memberCandidateMapper = memberCandidateMapper;
        this.groupKnowledgeMapper = groupKnowledgeMapper;
        this.memberProfileMapper = memberProfileMapper;
        this.publishLogMapper = publishLogMapper;
    }

    @Transactional
    public FormalKnowledgePublishResponse publishKnowledge(FormalKnowledgePublishRequest request) {
        validatePublishRequest(request);
        long published = 0;
        long skipped = 0;
        for (Long candidateId : request.candidateIds()) {
            ChatKnowledgeCandidateEntity candidate = knowledgeCandidateMapper.selectById(candidateId);
            if (candidate == null
                    || !request.groupId().equals(candidate.getGroupId())
                    || !ChatCandidateStatus.APPROVED.name().equals(candidate.getStatus())
                    || hasKnowledgeBySource(candidateId)) {
                skipped++;
                continue;
            }
            ChatGroupKnowledgeEntity entity = new ChatGroupKnowledgeEntity();
            entity.setGroupId(candidate.getGroupId());
            entity.setSourceCandidateId(candidate.getId());
            entity.setKnowledgeType(candidate.getCandidateType());
            entity.setTitle(limit(candidate.getTitle(), 100));
            entity.setContent(candidate.getContent());
            entity.setEvidenceText(candidate.getEvidenceText());
            entity.setStatus(FormalKnowledgeStatus.ACTIVE.name());
            entity.setEnabled(true);
            entity.setVersion(1);
            entity.setCreatedBy(operator(request.operator()));
            groupKnowledgeMapper.insert(entity);
            recordLog("KNOWLEDGE_CANDIDATE", candidate.getId(),
                    KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name(), entity.getId(),
                    KnowledgePublishAction.PUBLISH, request.operator(), request.comment());
            published++;
        }
        return new FormalKnowledgePublishResponse(published, skipped, "SUCCESS");
    }

    @Transactional
    public FormalKnowledgePublishResponse publishMemberProfiles(FormalKnowledgePublishRequest request) {
        validatePublishRequest(request);
        long published = 0;
        long skipped = 0;
        for (Long candidateId : request.candidateIds()) {
            ChatMemberCandidateEntity candidate = memberCandidateMapper.selectById(candidateId);
            if (candidate == null
                    || !request.groupId().equals(candidate.getGroupId())
                    || !ChatCandidateStatus.APPROVED.name().equals(candidate.getStatus())
                    || hasMemberProfileBySource(candidateId)) {
                skipped++;
                continue;
            }
            ChatMemberProfileEntity entity = new ChatMemberProfileEntity();
            entity.setGroupId(candidate.getGroupId());
            entity.setSourceMemberCandidateId(candidate.getId());
            entity.setSenderUid(candidate.getSenderUid());
            entity.setSenderUin(candidate.getSenderUin());
            entity.setSenderName(candidate.getSenderName());
            entity.setProfileText(buildProfileText(candidate));
            entity.setMessageCount(value(candidate.getMessageCount()));
            entity.setRawMessageCount(value(candidate.getRawMessageCount()));
            entity.setActiveDays(candidate.getActiveDays() == null ? 0 : candidate.getActiveDays());
            entity.setMentionCount(value(candidate.getMentionCount()));
            entity.setReplyCount(value(candidate.getReplyCount()));
            entity.setRepliedByCount(value(candidate.getRepliedByCount()));
            entity.setSessionCount(value(candidate.getSessionCount()));
            entity.setStatus(FormalKnowledgeStatus.ACTIVE.name());
            entity.setEnabled(true);
            entity.setCreatedBy(operator(request.operator()));
            memberProfileMapper.insert(entity);
            recordLog("MEMBER_CANDIDATE", candidate.getId(),
                    KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name(), entity.getId(),
                    KnowledgePublishAction.PUBLISH, request.operator(), request.comment());
            published++;
        }
        return new FormalKnowledgePublishResponse(published, skipped, "SUCCESS");
    }

    public List<ChatGroupKnowledgeEntity> findKnowledge(String groupId, Boolean enabled) {
        LambdaQueryWrapper<ChatGroupKnowledgeEntity> wrapper = new LambdaQueryWrapper<ChatGroupKnowledgeEntity>()
                .eq(hasText(groupId), ChatGroupKnowledgeEntity::getGroupId, groupId)
                .eq(enabled != null, ChatGroupKnowledgeEntity::getEnabled, enabled)
                .orderByDesc(ChatGroupKnowledgeEntity::getUpdatedAt)
                .orderByAsc(ChatGroupKnowledgeEntity::getId);
        return groupKnowledgeMapper.selectList(wrapper);
    }

    public List<ChatMemberProfileEntity> findMemberProfiles(String groupId, Boolean enabled) {
        LambdaQueryWrapper<ChatMemberProfileEntity> wrapper = new LambdaQueryWrapper<ChatMemberProfileEntity>()
                .eq(hasText(groupId), ChatMemberProfileEntity::getGroupId, groupId)
                .eq(enabled != null, ChatMemberProfileEntity::getEnabled, enabled)
                .orderByDesc(ChatMemberProfileEntity::getUpdatedAt)
                .orderByAsc(ChatMemberProfileEntity::getId);
        return memberProfileMapper.selectList(wrapper);
    }

    @Transactional
    public ChatGroupKnowledgeEntity setKnowledgeEnabled(
            Long id,
            boolean enabled,
            String operator,
            String comment) {
        ChatGroupKnowledgeEntity entity = groupKnowledgeMapper.selectById(id);
        if (entity == null) {
            throw new InvalidChatCandidateRequestException("knowledge not found");
        }
        entity.setEnabled(enabled);
        entity.setStatus(enabled ? FormalKnowledgeStatus.ACTIVE.name() : FormalKnowledgeStatus.DISABLED.name());
        groupKnowledgeMapper.updateById(entity);
        recordLog("GROUP_KNOWLEDGE", id, KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name(), id,
                enabled ? KnowledgePublishAction.ENABLE : KnowledgePublishAction.DISABLE,
                operator, comment);
        return entity;
    }

    @Transactional
    public ChatMemberProfileEntity setMemberProfileEnabled(
            Long id,
            boolean enabled,
            String operator,
            String comment) {
        ChatMemberProfileEntity entity = memberProfileMapper.selectById(id);
        if (entity == null) {
            throw new InvalidChatCandidateRequestException("member profile not found");
        }
        entity.setEnabled(enabled);
        entity.setStatus(enabled ? FormalKnowledgeStatus.ACTIVE.name() : FormalKnowledgeStatus.DISABLED.name());
        memberProfileMapper.updateById(entity);
        recordLog("MEMBER_PROFILE", id, KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name(), id,
                enabled ? KnowledgePublishAction.ENABLE : KnowledgePublishAction.DISABLE,
                operator, comment);
        return entity;
    }

    public String buildProfileText(ChatMemberCandidateEntity candidate) {
        String name = hasText(candidate.getSenderName()) ? candidate.getSenderName() : "unknown";
        return "Member " + name
                + " messages=" + value(candidate.getMessageCount())
                + ", activeDays=" + (candidate.getActiveDays() == null ? 0 : candidate.getActiveDays())
                + ", mentions=" + value(candidate.getMentionCount())
                + ", replies=" + value(candidate.getReplyCount())
                + ", repliedBy=" + value(candidate.getRepliedByCount())
                + ", sessions=" + value(candidate.getSessionCount())
                + ". Reason: " + safe(candidate.getCandidateReason());
    }

    private boolean hasKnowledgeBySource(Long sourceCandidateId) {
        return groupKnowledgeMapper.selectOne(new LambdaQueryWrapper<ChatGroupKnowledgeEntity>()
                .eq(ChatGroupKnowledgeEntity::getSourceCandidateId, sourceCandidateId)
                .last("LIMIT 1")) != null;
    }

    private boolean hasMemberProfileBySource(Long sourceCandidateId) {
        return memberProfileMapper.selectOne(new LambdaQueryWrapper<ChatMemberProfileEntity>()
                .eq(ChatMemberProfileEntity::getSourceMemberCandidateId, sourceCandidateId)
                .last("LIMIT 1")) != null;
    }

    private void recordLog(
            String sourceType,
            Long sourceId,
            String targetType,
            Long targetId,
            KnowledgePublishAction action,
            String operator,
            String comment) {
        ChatKnowledgePublishLogEntity log = new ChatKnowledgePublishLogEntity();
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action.name());
        log.setOperator(operator(operator));
        log.setComment(limit(comment, 512));
        publishLogMapper.insert(log);
    }

    private void validatePublishRequest(FormalKnowledgePublishRequest request) {
        if (request == null || !hasText(request.groupId())) {
            throw new InvalidChatCandidateRequestException("groupId is required");
        }
        if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
            throw new InvalidChatCandidateRequestException("candidateIds is required");
        }
    }

    private String operator(String value) {
        return hasText(value) ? limit(value.strip(), 128) : DEFAULT_OPERATOR;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safe(String value) {
        return hasText(value) ? value.strip() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
