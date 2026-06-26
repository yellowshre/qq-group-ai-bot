package com.yh.qqbot.service.rate;

import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.enums.RateBucket;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitService implements RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate rateRedisTemplate;
    private final QqBotProperties properties;

    public RedisRateLimitService(
            @Qualifier("rateStringRedisTemplate") StringRedisTemplate rateRedisTemplate,
            QqBotProperties properties) {
        this.rateRedisTemplate = rateRedisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean preConsume(String groupId, RateBucket bucket) {
        int limit = limitOf(bucket);
        if (limit <= 0) {
            return false;
        }

        String key = keyOf(groupId, bucket);
        try {
            Long value = rateRedisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                rateRedisTemplate.expire(key, WINDOW);
            }
            return value != null && value <= limit;
        } catch (Exception ex) {
            log.warn("Rate pre-consume failed. groupId={}, bucket={}", groupId, bucket, ex);
            return false;
        }
    }

    private int limitOf(RateBucket bucket) {
        return switch (bucket) {
            case EMOJI -> properties.getRateLimit().getEmojiPerMinute();
            case PASSIVE_CHAT -> properties.getRateLimit().getPassiveChatPerMinute();
            case ACTIVE_CHAT -> properties.getRateLimit().getActiveChatPerMinute();
        };
    }

    private String keyOf(String groupId, RateBucket bucket) {
        return switch (bucket) {
            case EMOJI -> RedisKeys.emojiRate(groupId);
            case PASSIVE_CHAT -> RedisKeys.passiveChatRate(groupId);
            case ACTIVE_CHAT -> RedisKeys.activeChatRate(groupId);
        };
    }
}
