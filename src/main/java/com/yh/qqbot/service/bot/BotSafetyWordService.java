package com.yh.qqbot.service.bot;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.SafetyWordMatchResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BotSafetyWordService {

    private final QqBotProperties properties;

    public BotSafetyWordService(QqBotProperties properties) {
        this.properties = properties;
    }

    public SafetyWordMatchResult match(String rawMessage) {
        boolean adminOnly = properties.getSafety().isAdminOnly();
        String offWord = firstMatched(rawMessage, properties.getSafety().getActiveChatOffWords());
        if (offWord != null) {
            return new SafetyWordMatchResult(true, SafetyWordMatchResult.ACTIVE_CHAT_OFF, offWord, adminOnly);
        }
        String onWord = firstMatched(rawMessage, properties.getSafety().getActiveChatOnWords());
        if (onWord != null) {
            return new SafetyWordMatchResult(true, SafetyWordMatchResult.ACTIVE_CHAT_ON, onWord, adminOnly);
        }
        return SafetyWordMatchResult.none(adminOnly);
    }

    public boolean isActiveChatOffWordMatched(String rawMessage) {
        return SafetyWordMatchResult.ACTIVE_CHAT_OFF.equals(match(rawMessage).action());
    }

    public boolean isActiveChatOnWordMatched(String rawMessage) {
        return SafetyWordMatchResult.ACTIVE_CHAT_ON.equals(match(rawMessage).action());
    }

    private String firstMatched(String rawMessage, List<String> words) {
        if (!hasText(rawMessage) || words == null) {
            return null;
        }
        String text = rawMessage.strip();
        for (String word : words) {
            if (hasText(word) && text.contains(word.strip())) {
                return word.strip();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
