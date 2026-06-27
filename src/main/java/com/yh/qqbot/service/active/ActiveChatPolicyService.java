package com.yh.qqbot.service.active;

import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.ActiveChatPolicyRequest;
import com.yh.qqbot.dto.ActiveChatPolicyResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActiveChatPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ActiveChatPolicyService.class);
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final StringRedisTemplate rateRedisTemplate;
    private final QqBotProperties properties;

    public ActiveChatPolicyService(
            @Qualifier("rateStringRedisTemplate") StringRedisTemplate rateRedisTemplate,
            QqBotProperties properties) {
        this.rateRedisTemplate = rateRedisTemplate;
        this.properties = properties;
    }

    public ActiveChatPolicyResult evaluate(ActiveChatPolicyRequest request) {
        QqBotProperties.ActiveChat config = properties.getActiveChat();
        long cooldownSeconds = Math.max(1, config.getCooldownSeconds());
        long maxPerHour = Math.max(0, config.getMaxPerHour());

        if (!config.isEnabled()) {
            return reject(ActiveChatPolicyResult.ACTIVE_CHAT_DISABLED);
        }
        if (request.atBot()) {
            return reject(ActiveChatPolicyResult.AT_BOT);
        }
        if (request.botNicknameMatched()) {
            return reject(ActiveChatPolicyResult.BOT_NICKNAME_MATCHED);
        }
        if (!request.groupBotEnabled()) {
            return reject(ActiveChatPolicyResult.GROUP_DISABLED);
        }
        if (!request.activeChatEnabledInGroup()) {
            return reject(ActiveChatPolicyResult.GROUP_ACTIVE_CHAT_DISABLED);
        }
        if (request.adminCommandHit()) {
            return reject(ActiveChatPolicyResult.ADMIN_COMMAND);
        }

        String text = request.rawMessage() == null ? "" : request.rawMessage().strip();
        if (text.isBlank()) {
            return reject(ActiveChatPolicyResult.EMPTY_MESSAGE);
        }
        if (isPunctuationOnly(text)) {
            return reject(ActiveChatPolicyResult.PUNCTUATION_ONLY);
        }
        if (text.length() < Math.max(1, config.getMinMessageLength())) {
            return reject(ActiveChatPolicyResult.TOO_SHORT);
        }
        if (text.length() > Math.max(1, config.getMaxMessageLength())) {
            return reject(ActiveChatPolicyResult.TOO_LONG);
        }
        if (!config.isAllowAfterMemeSent() && request.memeAlreadySent()) {
            return reject(ActiveChatPolicyResult.MEME_ALREADY_SENT);
        }
        if (!config.isAllowAfterBotMessage() && request.lastMessageFromBot()) {
            return reject(ActiveChatPolicyResult.LAST_MESSAGE_FROM_BOT);
        }
        if (isCoolingDown(request.groupId())) {
            return reject(ActiveChatPolicyResult.COOLDOWN);
        }
        if (hourlyCount(request.groupId()) >= maxPerHour) {
            return reject(ActiveChatPolicyResult.HOURLY_LIMIT);
        }

        double probability = config.getRandomProbability();
        if (probability <= 0) {
            return ActiveChatPolicyResult.rejected(
                    ActiveChatPolicyResult.RANDOM_MISS, false, cooldownSeconds, maxPerHour);
        }
        if (probability < 1 && ThreadLocalRandom.current().nextDouble() >= probability) {
            return ActiveChatPolicyResult.rejected(
                    ActiveChatPolicyResult.RANDOM_MISS, false, cooldownSeconds, maxPerHour);
        }
        return ActiveChatPolicyResult.allowed(cooldownSeconds, maxPerHour);
    }

    public void markActiveChatSent(Long groupId) {
        if (groupId == null) {
            return;
        }
        QqBotProperties.ActiveChat config = properties.getActiveChat();
        String groupIdText = String.valueOf(groupId);
        String cooldownKey = RedisKeys.activeChatCooldown(groupIdText);
        String hourKey = RedisKeys.activeChatHour(groupIdText, currentHour());
        try {
            rateRedisTemplate.opsForValue().set(
                    cooldownKey,
                    "1",
                    Duration.ofSeconds(Math.max(1, config.getCooldownSeconds())));
            rateRedisTemplate.opsForValue().increment(hourKey);
            rateRedisTemplate.expire(hourKey, Duration.ofHours(2));
        } catch (Exception ex) {
            log.warn("Failed to mark active chat sent. groupId={}", groupId, ex);
        }
    }

    private ActiveChatPolicyResult reject(String rejectReason) {
        QqBotProperties.ActiveChat config = properties.getActiveChat();
        return ActiveChatPolicyResult.rejected(
                rejectReason,
                true,
                Math.max(1, config.getCooldownSeconds()),
                Math.max(0, config.getMaxPerHour()));
    }

    private boolean isCoolingDown(Long groupId) {
        if (groupId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(rateRedisTemplate.hasKey(RedisKeys.activeChatCooldown(String.valueOf(groupId))));
        } catch (Exception ex) {
            log.warn("Failed to check active chat cooldown. groupId={}", groupId, ex);
            return true;
        }
    }

    private long hourlyCount(Long groupId) {
        if (groupId == null) {
            return 0;
        }
        try {
            String value = rateRedisTemplate.opsForValue().get(
                    RedisKeys.activeChatHour(String.valueOf(groupId), currentHour()));
            return value == null || value.isBlank() ? 0 : Long.parseLong(value);
        } catch (Exception ex) {
            log.warn("Failed to check active chat hourly count. groupId={}", groupId, ex);
            return Long.MAX_VALUE;
        }
    }

    private String currentHour() {
        return LocalDateTime.now().format(HOUR_FORMATTER);
    }

    private boolean isPunctuationOnly(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isWhitespace(ch) && Character.isLetterOrDigit(ch)) {
                return false;
            }
            if (!Character.isWhitespace(ch) && Character.getType(ch) == Character.OTHER_LETTER) {
                return false;
            }
        }
        return true;
    }
}
