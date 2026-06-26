package com.yh.qqbot.service.chat;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.service.config.GroupConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GroupPersonaService {

    private static final Logger log = LoggerFactory.getLogger(GroupPersonaService.class);
    private static final String DEFAULT_PERSONA =
            "\u4f60\u662f\u4e00\u4e2a\u8bf4\u8bdd\u7b80\u77ed\u3001\u81ea\u7136\u3001\u7565\u5e26\u5410\u69fd\u4f46\u4e0d\u6076\u610f\u653b\u51fb\u4eba\u7684 QQ \u7fa4\u673a\u5668\u4eba\u3002";

    private final GroupConfigService groupConfigService;
    private final QqBotProperties properties;

    public GroupPersonaService(GroupConfigService groupConfigService, QqBotProperties properties) {
        this.groupConfigService = groupConfigService;
        this.properties = properties;
    }

    public String getPersona(Long groupId) {
        if (groupId == null) {
            return defaultPersona();
        }
        try {
            GroupConfigSnapshot config = groupConfigService.getConfig(String.valueOf(groupId));
            if (config != null && hasText(config.persona())) {
                return config.persona().strip();
            }
        } catch (Exception ex) {
            log.warn("Failed to load group persona, fallback to default. groupId={}", groupId, ex);
        }
        return defaultPersona();
    }

    private String defaultPersona() {
        String configured = properties.getDefaultPersona();
        return hasText(configured) ? configured.strip() : DEFAULT_PERSONA;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
