package com.yh.qqbot.service.rate;

import com.yh.qqbot.enums.RateBucket;

public interface RateLimitService {

    boolean preConsume(String groupId, RateBucket bucket);

    default boolean preConsumeEmoji(String groupId) {
        return preConsume(groupId, RateBucket.EMOJI);
    }

    default boolean preConsumePassiveChat(String groupId) {
        return preConsume(groupId, RateBucket.PASSIVE_CHAT);
    }

    default boolean preConsumeActiveChat(String groupId) {
        return preConsume(groupId, RateBucket.ACTIVE_CHAT);
    }
}
