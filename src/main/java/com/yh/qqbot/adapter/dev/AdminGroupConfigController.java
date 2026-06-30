package com.yh.qqbot.adapter.dev;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.AdminGroupConfigListResponse;
import com.yh.qqbot.dto.AdminGroupConfigUpdateRequest;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.entity.GroupConfigEntity;
import com.yh.qqbot.enums.MemoryMode;
import com.yh.qqbot.mapper.GroupConfigMapper;
import com.yh.qqbot.service.config.GroupConfigService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/admin/groups")
public class AdminGroupConfigController {

    private final QqBotProperties properties;
    private final GroupConfigMapper groupConfigMapper;
    private final GroupConfigService groupConfigService;

    public AdminGroupConfigController(
            QqBotProperties properties,
            GroupConfigMapper groupConfigMapper,
            GroupConfigService groupConfigService) {
        this.properties = properties;
        this.groupConfigMapper = groupConfigMapper;
        this.groupConfigService = groupConfigService;
    }

    @GetMapping
    public AdminGroupConfigListResponse list() {
        List<GroupConfigSnapshot> configuredGroups = groupConfigMapper
                .selectList(new LambdaQueryWrapper<GroupConfigEntity>().orderByAsc(GroupConfigEntity::getGroupId))
                .stream()
                .map(entity -> groupConfigService.getConfig(String.valueOf(entity.getGroupId())))
                .toList();
        return new AdminGroupConfigListResponse(allowedGroupIds(), configuredGroups);
    }

    @GetMapping("/{groupId}")
    public GroupConfigSnapshot get(@PathVariable String groupId) {
        return groupConfigService.getConfig(normalizeGroupId(groupId));
    }

    @PutMapping("/{groupId}")
    public GroupConfigSnapshot update(
            @PathVariable String groupId,
            @RequestBody AdminGroupConfigUpdateRequest request) {
        String normalizedGroupId = normalizeGroupId(groupId);
        AdminGroupConfigUpdateRequest safeRequest = request == null
                ? new AdminGroupConfigUpdateRequest(null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null)
                : request;
        return groupConfigService.updateConfig(normalizedGroupId, current -> apply(current, safeRequest));
    }

    private GroupConfigSnapshot apply(GroupConfigSnapshot current, AdminGroupConfigUpdateRequest request) {
        return new GroupConfigSnapshot(
                current.groupId(),
                request.botOn() == null ? current.botOn() : request.botOn(),
                request.enableChat() == null ? current.enableChat() : request.enableChat(),
                request.enableMeme() == null ? current.enableMeme() : request.enableMeme(),
                request.enablePassiveChat() == null ? current.enablePassiveChat() : request.enablePassiveChat(),
                request.enableAutoJoin() == null ? current.enableAutoJoin() : request.enableAutoJoin(),
                positiveOrCurrent(request.activeCooldownSeconds(), current.activeCooldownSeconds()),
                nonNegativeOrCurrent(request.activeMaxPerHour(), current.activeMaxPerHour()),
                nonNegativeOrCurrent(request.activeMaxPerDay(), current.activeMaxPerDay()),
                request.safeWord() == null ? current.safeWord() : blankToNull(request.safeWord()),
                request.safeWordReply() == null ? current.safeWordReply() : request.safeWordReply().strip(),
                request.persona() == null ? current.persona() : request.persona().strip(),
                parseMemoryMode(request.memoryMode(), current.memoryMode()),
                request.enableKnowledgeContext() == null
                        ? current.enableKnowledgeContext()
                        : request.enableKnowledgeContext(),
                request.enableMemeKnowledge() == null ? current.enableMemeKnowledge() : request.enableMemeKnowledge(),
                request.enablePassiveChatKnowledge() == null
                        ? current.enablePassiveChatKnowledge()
                        : request.enablePassiveChatKnowledge(),
                request.enableActiveChatKnowledge() == null
                        ? current.enableActiveChatKnowledge()
                        : request.enableActiveChatKnowledge());
    }

    private List<String> allowedGroupIds() {
        Set<String> result = new LinkedHashSet<>();
        for (String value : properties.getOnebot().getAllowedGroupIds()) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String part : value.split(",")) {
                if (part != null && !part.isBlank()) {
                    result.add(part.strip());
                }
            }
        }
        return new ArrayList<>(result);
    }

    private String normalizeGroupId(String groupId) {
        if (groupId == null || groupId.isBlank() || !groupId.strip().matches("\\d+")) {
            throw new IllegalArgumentException("groupId must be numeric");
        }
        return groupId.strip();
    }

    private long positiveOrCurrent(Long value, long current) {
        return value == null ? current : Math.max(1, value);
    }

    private long nonNegativeOrCurrent(Long value, long current) {
        return value == null ? current : Math.max(0, value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private MemoryMode parseMemoryMode(String value, MemoryMode current) {
        if (value == null || value.isBlank()) {
            return current == null ? MemoryMode.SHORT : current;
        }
        try {
            return MemoryMode.valueOf(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("memoryMode is invalid");
        }
    }
}
