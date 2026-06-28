package com.yh.qqbot.chat.history.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryParsedMessage(
        String messageId,
        Long seq,
        LocalDateTime messageTime,
        String senderUid,
        String senderUin,
        String senderName,
        String senderGroupCard,
        String messageType,
        String rawText,
        boolean system,
        boolean recalled,
        boolean hasResource,
        List<String> elementTypes,
        JsonNode contentNode,
        JsonNode rawJson
) {
}
