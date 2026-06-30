package com.yh.qqbot.chat.history.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatImportBatchSummary;
import com.yh.qqbot.chat.history.entity.ChatImportBatchEntity;
import com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatImportBatchController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ChatImportBatchMapper batchMapper;

    public ChatImportBatchController(ChatImportBatchMapper batchMapper) {
        this.batchMapper = batchMapper;
    }

    @GetMapping("/import-batches")
    public List<ChatImportBatchSummary> importBatches(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        LambdaQueryWrapper<ChatImportBatchEntity> wrapper = new LambdaQueryWrapper<>();
        if (hasText(groupId)) {
            wrapper.eq(ChatImportBatchEntity::getGroupId, groupId.strip());
        }
        if (hasText(status)) {
            wrapper.eq(ChatImportBatchEntity::getStatus, status.strip());
        }
        wrapper.orderByDesc(ChatImportBatchEntity::getCreatedAt)
                .last("limit " + normalizeLimit(limit));
        return batchMapper.selectList(wrapper).stream()
                .map(this::summary)
                .toList();
    }

    private ChatImportBatchSummary summary(ChatImportBatchEntity entity) {
        return new ChatImportBatchSummary(
                entity.getId(),
                entity.getGroupId(),
                entity.getSourceFile(),
                entity.getChatName(),
                entity.getExporterName(),
                entity.getExporterVersion(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTotalMessages(),
                entity.getRawCount(),
                entity.getCleanCount(),
                entity.getMentionCount(),
                entity.getReplyCount(),
                entity.getSessionCount(),
                entity.getMemberCount(),
                entity.getStatus(),
                trimError(entity.getErrorMessage()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String trimError(String errorMessage) {
        if (!hasText(errorMessage)) {
            return null;
        }
        String normalized = errorMessage.strip();
        return normalized.length() > 240 ? normalized.substring(0, 240) + "..." : normalized;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
