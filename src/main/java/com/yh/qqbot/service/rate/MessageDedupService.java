package com.yh.qqbot.service.rate;

import com.yh.qqbot.dto.BotGroupMessage;

public interface MessageDedupService {

    boolean firstSeen(BotGroupMessage message);
}
