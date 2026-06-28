package com.yh.qqbot.chat.history.dto;

import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageMentionEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import java.util.List;

public record ChatMessageCleanResult(
        ChatCleanMessageEntity cleanMessage,
        List<ChatMessageMentionEntity> mentions,
        ChatMessageReplyEntity reply
) {
}
