package com.yh.qqbot.service.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.entity.ChatSummaryEntity;
import com.yh.qqbot.enums.MemoryMode;
import com.yh.qqbot.mapper.ChatSummaryMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisChatContextService implements ChatContextService {

    private static final Logger log = LoggerFactory.getLogger(RedisChatContextService.class);

    private final StringRedisTemplate chatRedisTemplate;
    private final ChatSummaryMapper chatSummaryMapper;
    private final QqBotProperties properties;

    public RedisChatContextService(
            @Qualifier("chatStringRedisTemplate") StringRedisTemplate chatRedisTemplate,
            ChatSummaryMapper chatSummaryMapper,
            QqBotProperties properties) {
        this.chatRedisTemplate = chatRedisTemplate;
        this.chatSummaryMapper = chatSummaryMapper;
        this.properties = properties;
    }

    @Override
    public String getRecentMessages(Long groupId) {
        if (groupId == null) {
            return NO_CONTEXT;
        }
        try {
            List<String> items = chatRedisTemplate.opsForList().range(contextKey(String.valueOf(groupId)), 0, -1);
            if (items == null || items.isEmpty()) {
                return NO_CONTEXT;
            }
            List<String> visibleItems = items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .toList();
            return visibleItems.isEmpty() ? NO_CONTEXT : String.join("\n", visibleItems);
        } catch (Exception ex) {
            log.warn("Failed to load recent chat context. groupId={}", groupId, ex);
            return NO_CONTEXT;
        }
    }

    @Override
    public void appendUserMessage(Long groupId, Long userId, String text) {
        String roleName = userId == null ? "\u7528\u6237" : "\u7528\u6237" + userId;
        appendMessage(groupId, roleName, text);
    }

    @Override
    public void appendBotReply(Long groupId, String botName, String replyText) {
        appendMessage(groupId, hasText(botName) ? botName : "\u673a\u5668\u4eba", replyText);
    }

    @Override
    public void appendMessage(Long groupId, String roleName, String content) {
        if (groupId == null || !hasText(content)) {
            return;
        }
        String key = contextKey(String.valueOf(groupId));
        try {
            chatRedisTemplate.opsForList().rightPush(key, formatLine(roleName, content));
            chatRedisTemplate.opsForList().trim(key, -contextMaxSize(), -1);
            chatRedisTemplate.expire(key, contextTtl());
        } catch (Exception ex) {
            log.warn("Failed to append recent chat context. groupId={}", groupId, ex);
        }
    }

    @Override
    public List<String> loadHotContext(String groupId) {
        try {
            List<String> items = chatRedisTemplate.opsForList().range(contextKey(groupId), 0, -1);
            return items == null ? List.of() : items;
        } catch (Exception ex) {
            log.warn("Failed to load hot context. groupId={}", groupId, ex);
            return List.of();
        }
    }

    @Override
    public List<String> loadColdSummaries(String groupId) {
        try {
            List<ChatSummaryEntity> entities = chatSummaryMapper.selectList(new LambdaQueryWrapper<ChatSummaryEntity>()
                    .eq(ChatSummaryEntity::getGroupId, Long.valueOf(groupId))
                    .orderByDesc(ChatSummaryEntity::getCreatedAt)
                    .last("LIMIT 30"));
            Collections.reverse(entities);
            return entities.stream()
                    .map(ChatSummaryEntity::getSummaryText)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to load cold summaries. groupId={}", groupId, ex);
            return List.of();
        }
    }

    @Override
    public List<String> recentMessagesForActiveDecision(String groupId) {
        try {
            List<String> items = chatRedisTemplate.opsForList().range(RedisKeys.recentChat(groupId), -20, -1);
            return items == null ? List.of() : items;
        } catch (Exception ex) {
            log.warn("Failed to load recent messages. groupId={}", groupId, ex);
            return List.of();
        }
    }

    @Override
    public void appendTurn(String groupId, String userText, String assistantText, MemoryMode memoryMode) {
        int turns = memoryMode == MemoryMode.LONG
                ? properties.getMemory().getLongTurns()
                : properties.getMemory().getShortTurns();
        String key = contextKey(groupId);
        try {
            chatRedisTemplate.opsForList().rightPush(key, formatLine("user", userText));
            chatRedisTemplate.opsForList().rightPush(key, formatLine("assistant", assistantText));
            chatRedisTemplate.opsForList().trim(key, -Math.min(turns * 2L, contextMaxSize()), -1);
            chatRedisTemplate.expire(key, contextTtl());
        } catch (Exception ex) {
            log.warn("Failed to append chat context. groupId={}", groupId, ex);
        }
    }

    @Override
    public void rememberRecentMessage(String groupId, String userText) {
        if (userText == null || userText.isBlank()) {
            return;
        }
        String key = RedisKeys.recentChat(groupId);
        try {
            chatRedisTemplate.opsForList().rightPush(key, Instant.now() + " " + userText);
            chatRedisTemplate.opsForList().trim(key, -20, -1);
            chatRedisTemplate.expire(key, properties.getMemory().getTtl());
        } catch (Exception ex) {
            log.warn("Failed to remember recent message. groupId={}", groupId, ex);
        }
    }

    @Override
    public void clearGroupMemory(String groupId) {
        try {
            chatRedisTemplate.delete(List.of(contextKey(groupId), RedisKeys.recentChat(groupId)));
        } catch (Exception ex) {
            log.warn("Failed to clear hot memory. groupId={}", groupId, ex);
        }
        try {
            chatSummaryMapper.delete(new LambdaQueryWrapper<ChatSummaryEntity>()
                    .eq(ChatSummaryEntity::getGroupId, Long.valueOf(groupId)));
        } catch (Exception ex) {
            log.warn("Failed to clear cold memory. groupId={}", groupId, ex);
        }
    }

    private String contextKey(String groupId) {
        return RedisKeys.chatContext(properties.getChatContext().getKeyPrefix(), groupId);
    }

    private long contextMaxSize() {
        return Math.max(1, properties.getChatContext().getMaxSize());
    }

    private Duration contextTtl() {
        return Duration.ofMinutes(Math.max(1, properties.getChatContext().getTtlMinutes()));
    }

    private String formatLine(String roleName, String content) {
        String safeRoleName = hasText(roleName) ? roleName.strip() : "\u672a\u77e5";
        String safeContent = content == null ? "" : content.strip();
        return limit(safeRoleName, 32) + "\uFF1A" + limit(safeContent, properties.getChatContext().getMaxMessageLength());
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        int safeMaxLength = Math.max(1, maxLength);
        return value.length() <= safeMaxLength ? value : value.substring(0, safeMaxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
