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
            return normalizeSnapshot(objectMapper.readValue(json, GroupConfigSnapshot.class));
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
        QqBotProperties.ActiveChat activeChat = properties.getActiveChat();
        return new GroupConfigSnapshot(
                groupId,
                true,
                true,
                true,
                true,
                false,
                Math.max(1, activeChat.getCooldownSeconds()),
                Math.max(0, activeChat.getMaxPerHour()),
                Math.max(0, activeChat.getMaxPerDay()),
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
                defaultTrue(entity.getEnableMeme()),
                defaultTrue(entity.getEnablePassiveChat()),
                Boolean.TRUE.equals(entity.getEnableAutoJoin()),
                positiveOrDefault(entity.getActiveCooldownSeconds(), properties.getActiveChat().getCooldownSeconds()),
                nonNegativeOrDefault(entity.getActiveHourLimit(), properties.getActiveChat().getMaxPerHour()),
                nonNegativeOrDefault(entity.getActiveDayLimit(), properties.getActiveChat().getMaxPerDay()),
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
        entity.setEnableMeme(snapshot.enableMeme());
        entity.setEnablePassiveChat(snapshot.enablePassiveChat());
        entity.setEnableAutoJoin(snapshot.enableAutoJoin());
        entity.setActiveCooldownSeconds(snapshot.activeCooldownSeconds());
        entity.setActiveHourLimit(snapshot.activeMaxPerHour());
        entity.setActiveDayLimit(snapshot.activeMaxPerDay());
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

    private GroupConfigSnapshot normalizeSnapshot(GroupConfigSnapshot snapshot) {
        QqBotProperties.ActiveChat activeChat = properties.getActiveChat();
        boolean likelyOldCache = !snapshot.enableMeme()
                && !snapshot.enablePassiveChat()
                && snapshot.activeCooldownSeconds() == 0
                && snapshot.activeMaxPerHour() == 0
                && snapshot.activeMaxPerDay() == 0;
        return new GroupConfigSnapshot(
                snapshot.groupId(),
                snapshot.botOn(),
                snapshot.enableChat(),
                likelyOldCache || snapshot.enableMeme(),
                likelyOldCache || snapshot.enablePassiveChat(),
                snapshot.enableAutoJoin(),
                positiveOrDefault(snapshot.activeCooldownSeconds(), activeChat.getCooldownSeconds()),
                nonNegativeOrDefault(snapshot.activeMaxPerHour(), activeChat.getMaxPerHour()),
                nonNegativeOrDefault(snapshot.activeMaxPerDay(), activeChat.getMaxPerDay()),
                snapshot.safeWord(),
                blankToDefault(snapshot.safeWordReply(), properties.getDefaultSafeReply()),
                blankToDefault(snapshot.persona(), properties.getDefaultPersona()),
                snapshot.memoryMode() == null ? MemoryMode.SHORT : snapshot.memoryMode());
    }

    private boolean defaultTrue(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    private long positiveOrDefault(Long value, long defaultValue) {
        return value == null || value <= 0 ? Math.max(1, defaultValue) : value;
    }

    private long positiveOrDefault(long value, long defaultValue) {
        return value <= 0 ? Math.max(1, defaultValue) : value;
    }

    private long nonNegativeOrDefault(Long value, long defaultValue) {
        return value == null || value < 0 ? Math.max(0, defaultValue) : value;
    }

    private long nonNegativeOrDefault(long value, long defaultValue) {
        return value < 0 ? Math.max(0, defaultValue) : value;
    }
}
