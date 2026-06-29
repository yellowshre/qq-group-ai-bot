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
        String offWord = firstMatched(rawMessage, configuredActiveChatOffWords());
        if (offWord != null) {
            return new SafetyWordMatchResult(true, SafetyWordMatchResult.ACTIVE_CHAT_OFF, offWord, adminOnly);
        }
        String onWord = firstMatched(rawMessage, configuredActiveChatOnWords());
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

    private List<String> configuredActiveChatOffWords() {
        List<String> aliases = properties.getCommandAliases().getActiveChatOffWords();
        return hasAnyText(aliases) ? aliases : properties.getSafety().getActiveChatOffWords();
    }

    private List<String> configuredActiveChatOnWords() {
        List<String> aliases = properties.getCommandAliases().getActiveChatOnWords();
        return hasAnyText(aliases) ? aliases : properties.getSafety().getActiveChatOnWords();
    }

    private boolean hasAnyText(List<String> words) {
        if (words == null || words.isEmpty()) {
            return false;
        }
        for (String word : words) {
            if (hasText(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
