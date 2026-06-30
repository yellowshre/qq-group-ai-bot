package com.yh.qqbot.adapter.dev;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.dto.AdminOpLogItem;
import com.yh.qqbot.dto.AdminTriggerLogItem;
import com.yh.qqbot.entity.AdminOpLogEntity;
import com.yh.qqbot.entity.TriggerLogEntity;
import com.yh.qqbot.mapper.AdminOpLogMapper;
import com.yh.qqbot.mapper.TriggerLogMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/admin/logs")
public class AdminLogController {

    private final TriggerLogMapper triggerLogMapper;
    private final AdminOpLogMapper adminOpLogMapper;

    public AdminLogController(TriggerLogMapper triggerLogMapper, AdminOpLogMapper adminOpLogMapper) {
        this.triggerLogMapper = triggerLogMapper;
        this.adminOpLogMapper = adminOpLogMapper;
    }

    @GetMapping("/triggers")
    public List<AdminTriggerLogItem> triggerLogs(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String messageId,
            @RequestParam(required = false) String responseType,
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        LambdaQueryWrapper<TriggerLogEntity> wrapper = new LambdaQueryWrapper<TriggerLogEntity>()
                .eq(groupId != null, TriggerLogEntity::getGroupId, groupId)
                .eq(userId != null, TriggerLogEntity::getUserId, userId)
                .eq(hasText(messageId), TriggerLogEntity::getMessageId, strip(messageId))
                .eq(hasText(responseType), TriggerLogEntity::getResponseType, strip(responseType))
                .eq(hasText(workflowType), TriggerLogEntity::getWorkflowType, strip(workflowType))
                .eq(success != null, TriggerLogEntity::getSuccess, success)
                .orderByDesc(TriggerLogEntity::getCreatedAt)
                .orderByDesc(TriggerLogEntity::getId)
                .last("LIMIT " + safeLimit(limit));
        return triggerLogMapper.selectList(wrapper).stream()
                .map(AdminTriggerLogItem::from)
                .toList();
    }

    @GetMapping("/admin-ops")
    public List<AdminOpLogItem> adminOpLogs(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long operatorUid,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        LambdaQueryWrapper<AdminOpLogEntity> wrapper = new LambdaQueryWrapper<AdminOpLogEntity>()
                .eq(groupId != null, AdminOpLogEntity::getGroupId, groupId)
                .eq(operatorUid != null, AdminOpLogEntity::getOperatorUid, operatorUid)
                .eq(hasText(operation), AdminOpLogEntity::getOperation, strip(operation))
                .orderByDesc(AdminOpLogEntity::getCreatedAt)
                .orderByDesc(AdminOpLogEntity::getId)
                .last("LIMIT " + safeLimit(limit));
        return adminOpLogMapper.selectList(wrapper).stream()
                .map(AdminOpLogItem::from)
                .toList();
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        return Math.max(1, Math.min(limit, 500));
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
