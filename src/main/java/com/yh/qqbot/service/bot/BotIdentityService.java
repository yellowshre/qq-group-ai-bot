package com.yh.qqbot.service.bot;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BotIdentityService {

    private static final String DEFAULT_DISPLAY_NAME = "\u5c0f\u9ec4";

    private final QqBotProperties properties;

    public BotIdentityService(QqBotProperties properties) {
        this.properties = properties;
    }

    public String getDisplayName() {
        String configured = properties.getIdentity().getDisplayName();
        if (hasText(configured)) {
            return configured.strip();
        }
        return properties.getNicknames().stream()
                .filter(this::hasText)
                .findFirst()
                .map(String::strip)
                .orElse(DEFAULT_DISPLAY_NAME);
    }

    public List<String> getAliases() {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addAll(aliases, properties.getIdentity().getAliases());
        addAll(aliases, properties.getNicknames());
        if (aliases.isEmpty()) {
            aliases.add(getDisplayName());
        }
        return new ArrayList<>(aliases);
    }

    public boolean isBotAliasMatched(String rawMessage) {
        return containsAny(rawMessage, getAliases());
    }

    public boolean isPassiveTriggerMatched(String rawMessage) {
        List<String> triggerWords = properties.getPassiveChat().getTriggerWords();
        return containsAny(rawMessage, triggerWords == null || triggerWords.isEmpty() ? getAliases() : triggerWords);
    }

    private void addAll(LinkedHashSet<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (hasText(value)) {
                target.add(value.strip());
            }
        }
    }

    private boolean containsAny(String rawMessage, List<String> words) {
        if (!hasText(rawMessage) || words == null) {
            return false;
        }
        String text = rawMessage.strip();
        for (String word : words) {
            if (hasText(word) && text.contains(word.strip())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
