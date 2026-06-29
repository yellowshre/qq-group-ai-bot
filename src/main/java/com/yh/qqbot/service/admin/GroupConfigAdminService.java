package com.yh.qqbot.service.admin;

import com.yh.qqbot.dto.GroupConfigAdminResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.service.config.GroupConfigService;
import org.springframework.stereotype.Service;

@Service
public class GroupConfigAdminService {

    private final GroupConfigService groupConfigService;

    public GroupConfigAdminService(GroupConfigService groupConfigService) {
        this.groupConfigService = groupConfigService;
    }

    public GroupConfigSnapshot getConfig(String groupId) {
        return groupConfigService.getConfig(groupId);
    }

    public GroupConfigAdminResult status(String groupId, String statusPrefix) {
        GroupConfigSnapshot config = getConfig(groupId);
        String prefix = hasText(statusPrefix) ? statusPrefix.strip() : "group config:";
        String text = """
                %s
                group: %s
                bot: %s
                meme A: %s
                passive chat B: %s
                active chat C: %s
                knowledge context: %s
                meme knowledge A: %s
                passive chat knowledge B: %s
                active chat knowledge C: %s
                active cooldown: %ds
                active hourly limit: %d
                active daily limit: %d
                persona: %s
                """.formatted(
                prefix,
                config.groupId(),
                onOff(config.botOn()),
                onOff(config.enableMeme()),
                onOff(config.passiveChatEnabled()),
                onOff(config.activeChatEnabled()),
                onOff(config.enableKnowledgeContext()),
                onOff(config.enableMemeKnowledge()),
                onOff(config.enablePassiveChatKnowledge()),
                onOff(config.enableActiveChatKnowledge()),
                config.activeCooldownSeconds(),
                config.activeMaxPerHour(),
                config.activeMaxPerDay(),
                hasText(config.persona()) ? "configured" : "default").strip();
        return new GroupConfigAdminResult("STATUS", "group config status", text);
    }

    public GroupConfigAdminResult setBotOn(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withBotOn(enabled));
        return result(enabled ? "BOT_ON" : "BOT_OFF", "bot_on=" + enabled, replyText);
    }

    public GroupConfigAdminResult setMemeEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withEnableMeme(enabled));
        return result(enabled ? "MEME_ON" : "MEME_OFF", "enable_meme=" + enabled, replyText);
    }

    public GroupConfigAdminResult setPassiveChatEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> enabled
                ? config.withEnableChat(true).withEnablePassiveChat(true)
                : config.withEnablePassiveChat(false));
        return result(
                enabled ? "PASSIVE_CHAT_ON" : "PASSIVE_CHAT_OFF",
                "enable_passive_chat=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setActiveChatEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> enabled
                ? config.withEnableChat(true).withEnableAutoJoin(true)
                : config.withEnableAutoJoin(false));
        return result(
                enabled ? "ACTIVE_CHAT_ON" : "ACTIVE_CHAT_OFF",
                "enable_auto_join=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setKnowledgeContextEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withEnableKnowledgeContext(enabled));
        return result(
                enabled ? "KNOWLEDGE_CONTEXT_ON" : "KNOWLEDGE_CONTEXT_OFF",
                "enable_knowledge_context=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setMemeKnowledgeEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withEnableMemeKnowledge(enabled));
        return result(
                enabled ? "MEME_KNOWLEDGE_ON" : "MEME_KNOWLEDGE_OFF",
                "enable_meme_knowledge=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setPassiveChatKnowledgeEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withEnablePassiveChatKnowledge(enabled));
        return result(
                enabled ? "PASSIVE_CHAT_KNOWLEDGE_ON" : "PASSIVE_CHAT_KNOWLEDGE_OFF",
                "enable_passive_chat_knowledge=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setActiveChatKnowledgeEnabled(String groupId, boolean enabled, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withEnableActiveChatKnowledge(enabled));
        return result(
                enabled ? "ACTIVE_CHAT_KNOWLEDGE_ON" : "ACTIVE_CHAT_KNOWLEDGE_OFF",
                "enable_active_chat_knowledge=" + enabled,
                replyText);
    }

    public GroupConfigAdminResult setActiveCooldownSeconds(String groupId, long seconds, String replyText) {
        long safeSeconds = Math.max(1, seconds);
        groupConfigService.updateConfig(groupId, config -> config.withActiveCooldownSeconds(safeSeconds));
        return result("ACTIVE_COOLDOWN_SET", "active_cooldown_seconds=" + safeSeconds, replyText);
    }

    public GroupConfigAdminResult setActiveHourLimit(String groupId, long limit, String replyText) {
        long safeLimit = Math.max(0, limit);
        groupConfigService.updateConfig(groupId, config -> config.withActiveMaxPerHour(safeLimit));
        return result("ACTIVE_HOUR_LIMIT_SET", "active_hour_limit=" + safeLimit, replyText);
    }

    public GroupConfigAdminResult setActiveDayLimit(String groupId, long limit, String replyText) {
        long safeLimit = Math.max(0, limit);
        groupConfigService.updateConfig(groupId, config -> config.withActiveMaxPerDay(safeLimit));
        return result("ACTIVE_DAY_LIMIT_SET", "active_day_limit=" + safeLimit, replyText);
    }

    public GroupConfigAdminResult setPersona(String groupId, String persona, String replyText) {
        String safePersona = persona == null ? "" : persona.strip();
        groupConfigService.updateConfig(groupId, config -> config.withPersona(safePersona));
        return result("PERSONA_SET", "persona updated", replyText);
    }

    public GroupConfigAdminResult clearPersona(String groupId, String replyText) {
        groupConfigService.updateConfig(groupId, config -> config.withPersona(""));
        return result("PERSONA_CLEAR", "persona cleared", replyText);
    }

    private GroupConfigAdminResult result(String operation, String detail, String replyText) {
        return new GroupConfigAdminResult(operation, detail, replyText);
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
