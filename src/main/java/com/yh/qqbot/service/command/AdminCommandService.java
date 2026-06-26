package com.yh.qqbot.service.command;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.enums.MemoryMode;
import com.yh.qqbot.service.config.GroupConfigService;
import com.yh.qqbot.service.context.ChatContextService;
import com.yh.qqbot.service.log.AdminOpLogService;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AdminCommandService {

    private final QqBotProperties properties;
    private final GroupConfigService groupConfigService;
    private final ChatContextService chatContextService;
    private final AdminOpLogService adminOpLogService;

    public AdminCommandService(
            QqBotProperties properties,
            GroupConfigService groupConfigService,
            ChatContextService chatContextService,
            AdminOpLogService adminOpLogService) {
        this.properties = properties;
        this.groupConfigService = groupConfigService;
        this.chatContextService = chatContextService;
        this.adminOpLogService = adminOpLogService;
    }

    public CommandHandleResult tryHandle(BotGroupMessage message, GroupConfigSnapshot config) {
        String text = message.effectiveText();
        if (!text.startsWith("#")) {
            return CommandHandleResult.notCommand();
        }

        String command = firstToken(text).toLowerCase(Locale.ROOT);
        if (!isKnownCommand(command)) {
            return CommandHandleResult.notCommand();
        }

        if (!isAdmin(message.userId())) {
            return CommandHandleResult.handled("DENIED", command, "你没有权限执行这个指令。");
        }

        CommandHandleResult result = switch (command) {
            case "#boton" -> update(message, "BOT_ON", "bot_on=true", snapshot -> snapshot.withBotOn(true), "机器人已开启。");
            case "#botoff" -> update(message, "BOT_OFF", "bot_on=false", snapshot -> snapshot.withBotOn(false), "机器人已关闭。");
            case "#chaton" -> update(message, "CHAT_ON", "enable_chat=true", snapshot -> snapshot.withEnableChat(true), "AI 对话已开启。");
            case "#chatoff" -> update(message, "CHAT_OFF", "enable_chat=false",
                    snapshot -> snapshot.withEnableChat(false).withEnableAutoJoin(false), "AI 对话和主动插话已关闭。");
            case "#autochaton" -> enableAutoChat(message, config);
            case "#autochatoff" -> update(message, "AUTO_CHAT_OFF", "enable_auto_join=false",
                    snapshot -> snapshot.withEnableAutoJoin(false), "主动插话已关闭。");
            case "#memshort" -> update(message, "MEM_SHORT", "memory_mode=SHORT",
                    snapshot -> snapshot.withMemoryMode(MemoryMode.SHORT), "已切换为短记忆模式。");
            case "#memlong" -> update(message, "MEM_LONG", "memory_mode=LONG",
                    snapshot -> snapshot.withMemoryMode(MemoryMode.LONG), "已切换为长记忆模式。");
            case "#clearchatctx" -> clearContext(message);
            case "#setsafeword" -> setSafeWord(message, text);
            case "#setsafereply" -> setSafeReply(message, text);
            default -> CommandHandleResult.notCommand();
        };

        if (result.handled() && result.operation() != null && !"DENIED".equals(result.operation())) {
            adminOpLogService.record(message.groupId(), message.userId(), result.operation(), result.detail());
        }
        return result;
    }

    private CommandHandleResult enableAutoChat(BotGroupMessage message, GroupConfigSnapshot config) {
        if (!config.enableChat()) {
            return CommandHandleResult.handled("AUTO_CHAT_ON_REJECTED", "enable_chat=false", "请先使用 #chaton 开启 AI 对话。");
        }
        return update(message, "AUTO_CHAT_ON", "enable_auto_join=true",
                snapshot -> snapshot.withEnableAutoJoin(true), "主动插话已开启。");
    }

    private CommandHandleResult clearContext(BotGroupMessage message) {
        chatContextService.clearGroupMemory(message.groupId());
        return CommandHandleResult.handled("CLEAR_CHAT_CONTEXT", "clear hot and cold context requested", "当前群记忆已清空。");
    }

    private CommandHandleResult setSafeWord(BotGroupMessage message, String text) {
        String value = text.length() > "#setsafeword".length()
                ? text.substring("#setsafeword".length()).strip()
                : "";
        if (value.equalsIgnoreCase("off") || value.isBlank()) {
            return update(message, "SAFE_WORD_OFF", "safe_word=null",
                    snapshot -> snapshot.withSafeWord(null), "安全词已关闭。");
        }
        return update(message, "SAFE_WORD_SET", "safe_word=" + value,
                snapshot -> snapshot.withSafeWord(value), "安全词已设置。");
    }

    private CommandHandleResult setSafeReply(BotGroupMessage message, String text) {
        String value = text.length() > "#setsafereply".length()
                ? text.substring("#setsafereply".length()).strip()
                : "";
        if (value.isBlank()) {
            return CommandHandleResult.handled("SAFE_REPLY_REJECTED", "blank reply", "安全词回复不能为空。");
        }
        return update(message, "SAFE_REPLY_SET", "safe_word_reply updated",
                snapshot -> snapshot.withSafeWordReply(value), "安全词回复已设置。");
    }

    private CommandHandleResult update(
            BotGroupMessage message,
            String operation,
            String detail,
            java.util.function.UnaryOperator<GroupConfigSnapshot> updater,
            String replyText) {
        groupConfigService.updateConfig(message.groupId(), updater);
        return CommandHandleResult.handled(operation, detail, replyText);
    }

    private boolean isAdmin(String userId) {
        return properties.getAdmins().stream()
                .filter(admin -> admin != null && !admin.isBlank())
                .anyMatch(admin -> admin.equals(userId));
    }

    private String firstToken(String text) {
        int index = text.indexOf(' ');
        return index < 0 ? text : text.substring(0, index);
    }

    private boolean isKnownCommand(String command) {
        return switch (command) {
            case "#boton", "#botoff", "#chaton", "#chatoff", "#autochaton", "#autochatoff",
                    "#memshort", "#memlong", "#clearchatctx", "#setsafeword", "#setsafereply" -> true;
            default -> false;
        };
    }
}
