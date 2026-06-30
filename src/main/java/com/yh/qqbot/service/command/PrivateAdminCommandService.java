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

/**
 * 私聊管理员指令服务。
 *
 * 负责处理管理员通过私聊发送的群配置控制命令，例如：
 * #群 251288204 状态
 * #群 251288204 开启表情包
 * #群 251288204 关闭主动插话
 */
@Service
public class PrivateAdminCommandService {

    /**
     * 私聊指令中的固定关键字和操作名。
     *
     * 这里使用常量集中管理，避免后面 switch 和 startsWith 中到处写硬编码字符串。
     */
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

    /**
     * 读取管理员列表、命令前缀、允许群、回复文案等配置。
     */
    private final QqBotProperties properties;

    /**
     * 真正执行群配置查询和修改的服务。
     */
    private final GroupConfigAdminService groupConfigAdminService;

    /**
     * 管理员操作日志服务，用于记录修改类操作。
     */
    private final AdminOpLogService adminOpLogService;

    /**
     * 成员排行私聊指令服务。
     *
     * 私聊排行命令也复用同一个私聊入口，所以这里先尝试交给它处理。
     */
    private final MemberRankCommandService memberRankCommandService;

    public PrivateAdminCommandService(
            QqBotProperties properties,
            GroupConfigAdminService groupConfigAdminService,
            AdminOpLogService adminOpLogService,
            MemberRankCommandService memberRankCommandService) {
        this.properties = properties;
        this.groupConfigAdminService = groupConfigAdminService;
        this.adminOpLogService = adminOpLogService;
        this.memberRankCommandService = memberRankCommandService;
    }

    /**
     * 私聊指令处理入口。
     *
     * 返回 handled=false 表示这条私聊不是本服务要处理的管理员命令；
     * 返回 handled=true 表示命令已处理，并可能带有需要私聊回复的 outboundMessage。
     */
    public CommandHandleResult tryHandle(BotPrivateMessage message) {
        String text = message == null ? "" : message.effectiveText();
        String prefix = commandPrefix();

        // 不是指定前缀开头的内容，直接认为不是管理员命令。
        if (!hasText(text) || !text.strip().startsWith(prefix)) {
            return CommandHandleResult.notCommand();
        }

        ParsedCommand parsed = parse(text.strip(), prefix);
        if (parsed == null) {
            return handled("UNKNOWN_PRIVATE_ADMIN_COMMAND", "parse failed", replies().getUnknownCommand());
        }

        // 只有配置中的管理员 QQ 才能执行私聊控制命令。
        if (!isAdmin(message.userId())) {
            return CommandHandleResult.notCommand();
        }

        if (!properties.getPrivateAdmin().isEnabled()) {
            return handled("PRIVATE_ADMIN_DISABLED", "private admin disabled", replies().getDisabled());
        }

        // 如果开启了限制，则只能控制白名单群。
        if (properties.getPrivateAdmin().isLimitToAllowedGroups() && !isAllowedGroup(parsed.groupId())) {
            return handled("GROUP_NOT_ALLOWED", "group not allowed", replies().getGroupNotAllowed());
        }

        // 排行类私聊命令先交给 MemberRankCommandService 尝试处理。
        CommandHandleResult rankCommand = memberRankCommandService.tryHandlePrivate(parsed.groupId(), parsed.operation());
        if (rankCommand != null && rankCommand.handled()) {
            return rankCommand;
        }

        GroupConfigAdminResult result = execute(parsed);
        if (result == null) {
            return handled("UNKNOWN_PRIVATE_ADMIN_COMMAND", parsed.operation(), replies().getUnknownCommand());
        }

        recordIfNeeded(parsed.groupId(), message.userId(), result);
        return handled(result.operation(), result.detail(), result.replyText());
    }

    /**
     * 执行不带复杂参数的群配置命令。
     *
     * switch 负责把中文操作映射到具体的 GroupConfigAdminService 方法。
     */
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

    /**
     * 执行带参数的命令。
     *
     * 例如：
     * 冷却 120
     * 每小时上限 5
     * 每天上限 20
     * 人设 xxx
     */
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

    /**
     * 解析私聊命令文本。
     *
     * 目标格式：
     * 前缀 + 群 + 群号 + 操作
     *
     * 例如：
     * #群 251288204 开启表情包
     */
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

    /**
     * 记录需要审计的管理员操作。
     *
     * 状态查询不修改配置，因此不记录操作日志。
     */
    private void recordIfNeeded(String groupId, String operatorUid, GroupConfigAdminResult result) {
        if (result == null || !hasText(result.operation()) || "STATUS".equals(result.operation())) {
            return;
        }
        adminOpLogService.record(groupId, operatorUid, result.operation(), result.detail());
    }

    /**
     * 统一构造已处理结果。
     */
    private CommandHandleResult handled(String operation, String detail, String replyText) {
        return CommandHandleResult.handled(operation, detail, replyText);
    }

    /**
     * 截取某个命令关键字后面的参数。
     */
    private String argumentAfter(String text, String prefix) {
        return text.length() <= prefix.length() ? "" : text.substring(prefix.length()).strip();
    }

    /**
     * 解析非负或正整数参数。
     *
     * requirePositive=true 时必须大于 0；
     * requirePositive=false 时允许为 0，但不能为负数。
     */
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

    /**
     * 查找字符串中第一个空白字符的位置。
     *
     * 用于把“群号”和“操作内容”拆开。
     */
    private int firstWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 判断当前私聊用户是否是管理员。
     */
    private boolean isAdmin(String userId) {
        if (!hasText(userId)) {
            return false;
        }
        return splitValues(properties.getAdmins()).contains(userId.strip());
    }

    /**
     * 判断目标群是否在 OneBot 允许群列表中。
     */
    private boolean isAllowedGroup(String groupId) {
        if (!hasText(groupId)) {
            return false;
        }
        return splitValues(properties.getOnebot().getAllowedGroupIds()).contains(groupId.strip());
    }

    /**
     * 把配置中的列表拆成 Set。
     *
     * 支持两种写法：
     * 1. 多个 YAML 列表项；
     * 2. 单个列表项中用英文逗号分隔多个值。
     */
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

    /**
     * 获取私聊管理员命令前缀。
     *
     * 如果配置为空，默认使用 #。
     */
    private String commandPrefix() {
        String prefix = properties.getPrivateAdmin().getCommandPrefix();
        return hasText(prefix) ? prefix.strip() : "#";
    }

    /**
     * 获取私聊管理员指令相关回复文案。
     */
    private QqBotProperties.Replies replies() {
        return properties.getPrivateAdmin().getReplies();
    }

    /**
     * 判断字符串是否有有效内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 解析后的私聊管理员命令。
     *
     * @param groupId 要操作的群号
     * @param operation 要执行的操作内容
     */
    private record ParsedCommand(String groupId, String operation) {
    }
}