package com.yh.qqbot.service.command;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotPrivateMessage;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigAdminResult;
import com.yh.qqbot.service.admin.GroupConfigAdminService;
import com.yh.qqbot.service.log.AdminOpLogService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PrivateAdminCommandService {

    private static final String GROUP_COMMAND = "\u7fa4";
    private static final String OP_STATUS = "\u72b6\u6001";
    private static final String OP_BOT_ON = "\u5f00\u542f\u673a\u5668\u4eba";
    private static final String OP_BOT_OFF = "\u5173\u95ed\u673a\u5668\u4eba";
    private static final String OP_BOT_QUIET = "\u673a\u5668\u4eba\u5b89\u9759";
    private static final String OP_BOT_RESUME = "\u673a\u5668\u4eba\u6062\u590d";
    private static final String OP_MEME_ON = "\u5f00\u542f\u8868\u60c5\u5305";
    private static final String OP_MEME_OFF = "\u5173\u95ed\u8868\u60c5\u5305";
    private static final String OP_PASSIVE_ON = "\u5f00\u542f\u88ab\u52a8\u804a\u5929";
    private static final String OP_PASSIVE_OFF = "\u5173\u95ed\u88ab\u52a8\u804a\u5929";
    private static final String OP_ACTIVE_ON = "\u5f00\u542f\u4e3b\u52a8\u63d2\u8bdd";
    private static final String OP_ACTIVE_OFF = "\u5173\u95ed\u4e3b\u52a8\u63d2\u8bdd";
    private static final String OP_KNOWLEDGE_ON = "\u5f00\u542f\u77e5\u8bc6\u5e93";
    private static final String OP_KNOWLEDGE_OFF = "\u5173\u95ed\u77e5\u8bc6\u5e93";
    private static final String OP_MEME_KNOWLEDGE_ON = "\u5f00\u542f\u8868\u60c5\u5305\u77e5\u8bc6";
    private static final String OP_MEME_KNOWLEDGE_OFF = "\u5173\u95ed\u8868\u60c5\u5305\u77e5\u8bc6";
    private static final String OP_PASSIVE_KNOWLEDGE_ON = "\u5f00\u542f\u88ab\u52a8\u77e5\u8bc6";
    private static final String OP_PASSIVE_KNOWLEDGE_OFF = "\u5173\u95ed\u88ab\u52a8\u77e5\u8bc6";
    private static final String OP_CHAT_KNOWLEDGE_ON = "\u5f00\u542f\u804a\u5929\u77e5\u8bc6";
    private static final String OP_CHAT_KNOWLEDGE_OFF = "\u5173\u95ed\u804a\u5929\u77e5\u8bc6";
    private static final String OP_ACTIVE_KNOWLEDGE_ON = "\u5f00\u542f\u4e3b\u52a8\u77e5\u8bc6";
    private static final String OP_ACTIVE_KNOWLEDGE_OFF = "\u5173\u95ed\u4e3b\u52a8\u77e5\u8bc6";
    private static final String OP_COOLDOWN = "\u51b7\u5374";
    private static final String OP_HOUR_LIMIT = "\u5c0f\u65f6\u4e0a\u9650";
    private static final String OP_HOURLY_LIMIT = "\u6bcf\u5c0f\u65f6\u4e0a\u9650";
    private static final String OP_DAY_LIMIT = "\u6bcf\u65e5\u4e0a\u9650";
    private static final String OP_DAILY_LIMIT = "\u6bcf\u5929\u4e0a\u9650";
    private static final String OP_PERSONA = "\u4eba\u8bbe";
    private static final String OP_CLEAR_PERSONA = "\u6e05\u7a7a\u4eba\u8bbe";

    private final QqBotProperties properties;
    private final GroupConfigAdminService groupConfigAdminService;
    private final AdminOpLogService adminOpLogService;

    public PrivateAdminCommandService(
            QqBotProperties properties,
            GroupConfigAdminService groupConfigAdminService,
            AdminOpLogService adminOpLogService) {
        this.properties = properties;
        this.groupConfigAdminService = groupConfigAdminService;
        this.adminOpLogService = adminOpLogService;
    }

    public CommandHandleResult tryHandle(BotPrivateMessage message) {
        String text = message == null ? "" : message.effectiveText();
        String prefix = commandPrefix();
        if (!hasText(text) || !text.strip().startsWith(prefix)) {
            return CommandHandleResult.notCommand();
        }

        ParsedCommand parsed = parse(text.strip(), prefix);
        if (parsed == null) {
            return handled("UNKNOWN_PRIVATE_ADMIN_COMMAND", "parse failed", replies().getUnknownCommand());
        }
        if (!isAdmin(message.userId())) {
            return CommandHandleResult.notCommand();
        }
        if (!properties.getPrivateAdmin().isEnabled()) {
            return handled("PRIVATE_ADMIN_DISABLED", "private admin disabled", replies().getDisabled());
        }
        if (properties.getPrivateAdmin().isLimitToAllowedGroups() && !isAllowedGroup(parsed.groupId())) {
            return handled("GROUP_NOT_ALLOWED", "group not allowed", replies().getGroupNotAllowed());
        }

        GroupConfigAdminResult result = execute(parsed);
        if (result == null) {
            return handled("UNKNOWN_PRIVATE_ADMIN_COMMAND", parsed.operation(), replies().getUnknownCommand());
        }
        recordIfNeeded(parsed.groupId(), message.userId(), result);
        return handled(result.operation(), result.detail(), result.replyText());
    }

    private GroupConfigAdminResult execute(ParsedCommand command) {
        String groupId = command.groupId();
        String operation = command.operation();
        String success = replies().getSuccess();
        return switch (operation) {
            case OP_STATUS -> groupConfigAdminService.status(groupId, replies().getStatusPrefix());
            case OP_BOT_ON, OP_BOT_RESUME -> groupConfigAdminService.setBotOn(groupId, true, success);
            case OP_BOT_OFF, OP_BOT_QUIET -> groupConfigAdminService.setBotOn(groupId, false, success);
            case OP_MEME_ON -> groupConfigAdminService.setMemeEnabled(groupId, true, success);
            case OP_MEME_OFF -> groupConfigAdminService.setMemeEnabled(groupId, false, success);
            case OP_PASSIVE_ON -> groupConfigAdminService.setPassiveChatEnabled(groupId, true, success);
            case OP_PASSIVE_OFF -> groupConfigAdminService.setPassiveChatEnabled(groupId, false, success);
            case OP_ACTIVE_ON -> groupConfigAdminService.setActiveChatEnabled(groupId, true, success);
            case OP_ACTIVE_OFF -> groupConfigAdminService.setActiveChatEnabled(groupId, false, success);
            case OP_KNOWLEDGE_ON -> groupConfigAdminService.setKnowledgeContextEnabled(groupId, true, success);
            case OP_KNOWLEDGE_OFF -> groupConfigAdminService.setKnowledgeContextEnabled(groupId, false, success);
            case OP_MEME_KNOWLEDGE_ON -> groupConfigAdminService.setMemeKnowledgeEnabled(groupId, true, success);
            case OP_MEME_KNOWLEDGE_OFF -> groupConfigAdminService.setMemeKnowledgeEnabled(groupId, false, success);
            case OP_PASSIVE_KNOWLEDGE_ON, OP_CHAT_KNOWLEDGE_ON ->
                    groupConfigAdminService.setPassiveChatKnowledgeEnabled(groupId, true, success);
            case OP_PASSIVE_KNOWLEDGE_OFF, OP_CHAT_KNOWLEDGE_OFF ->
                    groupConfigAdminService.setPassiveChatKnowledgeEnabled(groupId, false, success);
            case OP_ACTIVE_KNOWLEDGE_ON -> groupConfigAdminService.setActiveChatKnowledgeEnabled(groupId, true, success);
            case OP_ACTIVE_KNOWLEDGE_OFF -> groupConfigAdminService.setActiveChatKnowledgeEnabled(groupId, false, success);
            case OP_CLEAR_PERSONA -> groupConfigAdminService.clearPersona(groupId, success);
            default -> executeWithArgument(groupId, operation, success);
        };
    }

    private GroupConfigAdminResult executeWithArgument(String groupId, String operation, String success) {
        if (operation.startsWith(OP_COOLDOWN)) {
            Long value = parsePositiveLong(argumentAfter(operation, OP_COOLDOWN), true);
            return value == null ? null : groupConfigAdminService.setActiveCooldownSeconds(groupId, value, success);
        }
        if (operation.startsWith(OP_HOUR_LIMIT)) {
            Long value = parsePositiveLong(argumentAfter(operation, OP_HOUR_LIMIT), false);
            return value == null ? null : groupConfigAdminService.setActiveHourLimit(groupId, value, success);
        }
        if (operation.startsWith(OP_HOURLY_LIMIT)) {
            Long value = parsePositiveLong(argumentAfter(operation, OP_HOURLY_LIMIT), false);
            return value == null ? null : groupConfigAdminService.setActiveHourLimit(groupId, value, success);
        }
        if (operation.startsWith(OP_DAY_LIMIT)) {
            Long value = parsePositiveLong(argumentAfter(operation, OP_DAY_LIMIT), false);
            return value == null ? null : groupConfigAdminService.setActiveDayLimit(groupId, value, success);
        }
        if (operation.startsWith(OP_DAILY_LIMIT)) {
            Long value = parsePositiveLong(argumentAfter(operation, OP_DAILY_LIMIT), false);
            return value == null ? null : groupConfigAdminService.setActiveDayLimit(groupId, value, success);
        }
        if (operation.startsWith(OP_PERSONA)) {
            String persona = argumentAfter(operation, OP_PERSONA);
            return hasText(persona) ? groupConfigAdminService.setPersona(groupId, persona, success) : null;
        }
        return null;
    }

    private ParsedCommand parse(String text, String prefix) {
        String body = text.substring(prefix.length()).strip();
        if (!body.startsWith(GROUP_COMMAND)) {
            return null;
        }
        body = body.substring(GROUP_COMMAND.length()).strip();
        int split = firstWhitespace(body);
        if (split <= 0) {
            return null;
        }
        String groupId = body.substring(0, split).strip();
        String operation = body.substring(split).strip();
        if (!groupId.matches("\\d+") || !hasText(operation)) {
            return null;
        }
        return new ParsedCommand(groupId, operation);
    }

    private void recordIfNeeded(String groupId, String operatorUid, GroupConfigAdminResult result) {
        if (result == null || !hasText(result.operation()) || "STATUS".equals(result.operation())) {
            return;
        }
        adminOpLogService.record(groupId, operatorUid, result.operation(), result.detail());
    }

    private CommandHandleResult handled(String operation, String detail, String replyText) {
        return CommandHandleResult.handled(operation, detail, replyText);
    }

    private String argumentAfter(String text, String prefix) {
        return text.length() <= prefix.length() ? "" : text.substring(prefix.length()).strip();
    }

    private Long parsePositiveLong(String value, boolean requirePositive) {
        if (!hasText(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.strip());
            if (requirePositive && parsed <= 0) {
                return null;
            }
            if (!requirePositive && parsed < 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int firstWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isAdmin(String userId) {
        if (!hasText(userId)) {
            return false;
        }
        return splitValues(properties.getAdmins()).contains(userId.strip());
    }

    private boolean isAllowedGroup(String groupId) {
        if (!hasText(groupId)) {
            return false;
        }
        return splitValues(properties.getOnebot().getAllowedGroupIds()).contains(groupId.strip());
    }

    private Set<String> splitValues(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            for (String part : value.split(",")) {
                if (hasText(part)) {
                    result.add(part.strip());
                }
            }
        }
        return result;
    }

    private String commandPrefix() {
        String prefix = properties.getPrivateAdmin().getCommandPrefix();
        return hasText(prefix) ? prefix.strip() : "#";
    }

    private QqBotProperties.Replies replies() {
        return properties.getPrivateAdmin().getReplies();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedCommand(String groupId, String operation) {
    }
}
