package com.yh.qqbot.chat.history.service.candidate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatCandidateStatus;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatCandidateQueryService {

    private final ChatKnowledgeCandidateMapper knowledgeCandidateMapper;
    private final ChatMemberCandidateMapper memberCandidateMapper;

    public ChatCandidateQueryService(
            ChatKnowledgeCandidateMapper knowledgeCandidateMapper,
            ChatMemberCandidateMapper memberCandidateMapper) {
        this.knowledgeCandidateMapper = knowledgeCandidateMapper;
        this.memberCandidateMapper = memberCandidateMapper;
    }

    public List<ChatKnowledgeCandidateEntity> findKnowledgeCandidates(Long batchId, String groupId, String status) {
        LambdaQueryWrapper<ChatKnowledgeCandidateEntity> wrapper = new LambdaQueryWrapper<ChatKnowledgeCandidateEntity>()
                .eq(batchId != null, ChatKnowledgeCandidateEntity::getBatchId, batchId)
                .eq(groupId != null && !groupId.isBlank(), ChatKnowledgeCandidateEntity::getGroupId, groupId)
                .orderByDesc(ChatKnowledgeCandidateEntity::getHitCount)
                .orderByDesc(ChatKnowledgeCandidateEntity::getMemberCount)
                .orderByAsc(ChatKnowledgeCandidateEntity::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ChatKnowledgeCandidateEntity::getStatus, parseStatus(status).name());
        }
        return knowledgeCandidateMapper.selectList(wrapper);
    }

    public List<ChatMemberCandidateEntity> findMemberCandidates(Long batchId, String groupId, String status) {
        LambdaQueryWrapper<ChatMemberCandidateEntity> wrapper = new LambdaQueryWrapper<ChatMemberCandidateEntity>()
                .eq(batchId != null, ChatMemberCandidateEntity::getBatchId, batchId)
                .eq(groupId != null && !groupId.isBlank(), ChatMemberCandidateEntity::getGroupId, groupId)
                .orderByDesc(ChatMemberCandidateEntity::getScore)
                .orderByAsc(ChatMemberCandidateEntity::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ChatMemberCandidateEntity::getStatus, parseStatus(status).name());
        }
        return memberCandidateMapper.selectList(wrapper);
    }

    private ChatCandidateStatus parseStatus(String value) {
        try {
            return ChatCandidateStatus.valueOf(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidChatCandidateRequestException("status is invalid");
        }
    }
}
