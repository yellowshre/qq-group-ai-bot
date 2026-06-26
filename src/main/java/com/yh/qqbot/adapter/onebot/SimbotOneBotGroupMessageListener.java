package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotGroupMessage;
import java.time.Instant;
import java.util.List;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent;
import love.forte.simbot.component.onebot.v11.event.message.RawGroupMessageEvent;
import love.forte.simbot.component.onebot.v11.message.OneBotMessageContent;
import love.forte.simbot.component.onebot.v11.message.segment.OneBotAt;
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegment;
import love.forte.simbot.quantcat.common.annotations.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimbotOneBotGroupMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SimbotOneBotGroupMessageListener.class);

    private final OneBotInboundAdapter oneBotInboundAdapter;
    private final QqBotProperties properties;

    public SimbotOneBotGroupMessageListener(OneBotInboundAdapter oneBotInboundAdapter, QqBotProperties properties) {
        this.oneBotInboundAdapter = oneBotInboundAdapter;
        this.properties = properties;
    }

    @Listener(id = "qqbot-onebot-group-message-listener", priority = 0)
    public void onGroupMessage(OneBotGroupMessageEvent event) {
        String groupId = event.getGroupId().toString();
        String userId = event.getUserId().toString();
        if (isSelfMessage(userId)) {
            log.debug("Ignore self group message. groupId={}, userId={}", groupId, userId);
            return;
        }

        String rawMessage = blankToNull(event.getRawMessage());
        OneBotMessageContent content = event.getMessageContent();
        String plainText = content == null ? rawMessage : blankToNull(content.getPlainText());
        if (plainText == null) {
            plainText = rawMessage == null ? "" : rawMessage;
        }

        BotGroupMessage message = new BotGroupMessage(
                groupId,
                userId,
                event.getMessageId().toString(),
                rawMessage == null ? "" : rawMessage,
                plainText,
                atConfiguredBot(event, rawMessage),
                mentionedBotNickname(plainText),
                receivedAt(event.getSourceEvent())
        );
        oneBotInboundAdapter.handleGroupMessage(message);
    }

    private boolean isSelfMessage(String userId) {
        String botId = normalize(properties.getBotId());
        return botId != null && botId.equals(userId);
    }

    private boolean atConfiguredBot(OneBotGroupMessageEvent event, String rawMessage) {
        String botId = normalize(properties.getBotId());
        if (botId == null) {
            return false;
        }

        List<OneBotMessageSegment> segments = event.getSourceEvent().getMessage();
        if (segments != null && segments.stream()
                .filter(OneBotAt.class::isInstance)
                .map(OneBotAt.class::cast)
                .map(OneBotAt::getData)
                .anyMatch(data -> data != null && botId.equals(data.getQq()))) {
            return true;
        }

        return rawMessage != null && rawMessage.contains("[CQ:at,qq=" + botId);
    }

    private boolean mentionedBotNickname(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return properties.getNicknames().stream()
                .filter(nickname -> nickname != null && !nickname.isBlank())
                .anyMatch(text::contains);
    }

    private Instant receivedAt(RawGroupMessageEvent sourceEvent) {
        long time = sourceEvent == null ? 0 : sourceEvent.getTime();
        return time > 0 ? Instant.ofEpochSecond(time) : Instant.now();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
