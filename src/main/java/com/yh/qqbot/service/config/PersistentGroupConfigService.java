package com.yh.qqbot.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.entity.GroupConfigEntity;
import com.yh.qqbot.enums.MemoryMode;
import com.yh.qqbot.mapper.GroupConfigMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PersistentGroupConfigService implements GroupConfigService {

    private static final Logger log = LoggerFactory.getLogger(PersistentGroupConfigService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final GroupConfigMapper groupConfigMapper;
    private final StringRedisTemplate baseRedisTemplate;
    private final ObjectMapper objectMapper;
    private final QqBotProperties properties;
    private final ConcurrentMap<String, GroupConfigSnapshot> fallbackConfigs = new ConcurrentHashMap<>();

    public PersistentGroupConfigService(
            GroupConfigMapper groupConfigMapper,
            @Qualifier("baseStringRedisTemplate") StringRedisTemplate baseRedisTemplate,
            ObjectMapper objectMapper,
            QqBotProperties properties) {
        this.groupConfigMapper = groupConfigMapper;
        this.baseRedisTemplate = baseRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public GroupConfigSnapshot getConfig(String groupId) {
        GroupConfigSnapshot cached = readCache(groupId);
        if (cached != null) {
            return cached;
        }

        try {
            GroupConfigEntity entity = groupConfigMapper.selectById(Long.valueOf(groupId));
            GroupConfigSnapshot snapshot = entity == null ? createDefaultConfig(groupId) : fromEntity(entity);
            writeCache(snapshot);
            fallbackConfigs.put(groupId, snapshot);
            return snapshot;
        } catch (Exception ex) {
            log.warn("Failed to load group config, fallback to local snapshot. groupId={}", groupId, ex);
            return fallbackConfigs.computeIfAbsent(groupId, this::defaultConfig);
        }
    }

    @Override
    public GroupConfigSnapshot updateConfig(String groupId, UnaryOperator<GroupConfigSnapshot> updater) {
        GroupConfigSnapshot updated = updater.apply(getConfig(groupId));
        fallbackConfigs.put(groupId, updated);
        writeCache(updated);

        try {
            GroupConfigEntity entity = toEntity(updated);
            GroupConfigEntity exists = groupConfigMapper.selectById(entity.getGroupId());
            if (exists == null) {
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                groupConfigMapper.insert(entity);
            } else {
                entity.setUpdatedAt(LocalDateTime.now());
                groupConfigMapper.updateById(entity);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist group config, local fallback already updated. groupId={}", groupId, ex);
        }
        return updated;
    }

    private GroupConfigSnapshot readCache(String groupId) {
        try {
            String json = baseRedisTemplate.opsForValue().get(RedisKeys.groupConfig(groupId));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, GroupConfigSnapshot.class);
        } catch (Exception ex) {
            log.debug("Read group config cache failed. groupId={}", groupId, ex);
            return null;
        }
    }

    private void writeCache(GroupConfigSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            baseRedisTemplate.opsForValue().set(RedisKeys.groupConfig(snapshot.groupId()), json, CACHE_TTL);
        } catch (Exception ex) {
            log.debug("Write group config cache failed. groupId={}", snapshot.groupId(), ex);
        }
    }

    private GroupConfigSnapshot defaultConfig(String groupId) {
        return new GroupConfigSnapshot(
                groupId,
                true,
                true,
                false,
                null,
                properties.getDefaultSafeReply(),
                properties.getDefaultPersona(),
                MemoryMode.SHORT
        );
    }

    private GroupConfigSnapshot createDefaultConfig(String groupId) {
        GroupConfigSnapshot snapshot = defaultConfig(groupId);
        try {
            GroupConfigEntity entity = toEntity(snapshot);
            LocalDateTime now = LocalDateTime.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            groupConfigMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to create default group config. groupId={}", groupId, ex);
        }
        return snapshot;
    }

    private GroupConfigSnapshot fromEntity(GroupConfigEntity entity) {
        return new GroupConfigSnapshot(
                String.valueOf(entity.getGroupId()),
                Boolean.TRUE.equals(entity.getBotOn()),
                Boolean.TRUE.equals(entity.getEnableChat()),
                Boolean.TRUE.equals(entity.getEnableAutoJoin()),
                entity.getSafeWord(),
                blankToDefault(entity.getSafeWordReply(), properties.getDefaultSafeReply()),
                blankToDefault(entity.getPersona(), properties.getDefaultPersona()),
                parseMemoryMode(entity.getMemoryMode())
        );
    }

    private GroupConfigEntity toEntity(GroupConfigSnapshot snapshot) {
        GroupConfigEntity entity = new GroupConfigEntity();
        entity.setGroupId(Long.valueOf(snapshot.groupId()));
        entity.setBotOn(snapshot.botOn());
        entity.setEnableChat(snapshot.enableChat());
        entity.setEnableAutoJoin(snapshot.enableAutoJoin());
        entity.setSafeWord(snapshot.safeWord());
        entity.setSafeWordReply(snapshot.safeWordReply());
        entity.setPersona(snapshot.persona());
        entity.setMemoryMode(snapshot.memoryMode().name());
        return entity;
    }

    private MemoryMode parseMemoryMode(String value) {
        if (value == null || value.isBlank()) {
            return MemoryMode.SHORT;
        }
        try {
            return MemoryMode.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return MemoryMode.SHORT;
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
