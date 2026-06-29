package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.OutboundMessage;

public interface QqMessageSender {

    boolean sendGroupMessage(String groupId, OutboundMessage outboundMessage);

    boolean sendPrivateMessage(String userId, OutboundMessage outboundMessage);
}
