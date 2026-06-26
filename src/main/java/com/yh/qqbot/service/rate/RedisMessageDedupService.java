package com.yh.qqbot.service.rate;

import com.yh.qqbot.cache.RedisKeys;
import com.yh.qqbot.dto.BotGroupMessage;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageDedupService implements MessageDedupService {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageDedupService.class);
    private static final Duration DEDUP_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate baseRedisTemplate;

    public RedisMessageDedupService(@Qualifier("baseStringRedisTemplate") StringRedisTemplate baseRedisTemplate) {
        this.baseRedisTemplate = baseRedisTemplate;
    }

    @Override
    public boolean firstSeen(BotGroupMessage message) {
        try {
            Boolean result = baseRedisTemplate.opsForValue()
                    .setIfAbsent(RedisKeys.messageDedup(message.messageId()), "1", DEDUP_TTL);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            log.warn("Message dedup failed. messageId={}", message.messageId(), ex);
            return false;
        }
    }
}
