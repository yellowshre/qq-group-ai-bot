package com.yh.qqbot.service.command;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.enums.MemoryMode;
import com.yh.qqbot.service.config.GroupConfigService;
import com.yh.qqbot.service.context.ChatContextService;
import com.yh.qqbot.service.log.AdminOpLogService;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AdminCommandService {

    private static final String CMD_STATUS = "\u0023\u72b6\u6001";
    private static final String CMD_ACTIVE_ON = "\u0023\u5f00\u542f\u4e3b\u52a8\u63d2\u8bdd";
    private static final String CMD_ACTIVE_OFF = "\u0023\u5173\u95ed\u4e3b\u52a8\u63d2\u8bdd";
    private static final String CMD_MEME_ON = "\u0023\u5f00\u542f\u8868\u60c5\u5305";
    private static final String CMD_MEME_OFF = "\u0023\u5173\u95ed\u8868\u60c5\u5305";
    private static final String CMD_BOT_QUIET = "\u0023\u673a\u5668\u4eba\u5b89\u9759";
    private static final String CMD_BOT_RESUME = "\u0023\u673a\u5668\u4eba\u6062\u590d";
    private static final String CMD_KNOWLEDGE_ON = "\u0023\u5f00\u542f\u77e5\u8bc6\u5e93";
    private static final String CMD_KNOWLEDGE_OFF = "\u0023\u5173\u95ed\u77e5\u8bc6\u5e93";
    private static final String CMD_MEME_KNOWLEDGE_ON = "\u0023\u5f00\u542f\u8868\u60c5\u5305\u77e5\u8bc6";
    private static final String CMD_MEME_KNOWLEDGE_OFF = "\u0023\u5173\u95ed\u8868\u60c5\u5305\u77e5\u8bc6";
    private static final String CMD_CHAT_KNOWLEDGE_ON = "\u0023\u5f00\u542f\u804a\u5929\u77e5\u8bc6";
    private static final String CMD_CHAT_KNOWLEDGE_OFF = "\u0023\u5173\u95ed\u804a\u5929\u77e5\u8bc6";
    private static final String CMD_ACTIVE_KNOWLEDGE_ON = "\u0023\u5f00\u542f\u4e3b\u52a8\u77e5\u8bc6";
    private static final String CMD_ACTIVE_KNOWLEDGE_OFF = "\u0023\u5173\u95ed\u4e3b\u52a8\u77e5\u8bc6";

    private final QqBotProperties properties;
    private final GroupConfigService groupConfigService;
    private final ChatContextService chatContextService;
    private final AdminOpLogService adminOpLogService;
    private final MemberRankCommandService memberRankCommandService;

    public AdminCommandService(
            QqBotProperties properties,
            GroupConfigService groupConfigService,
            ChatContextService chatContextService,
            AdminOpLogService adminOpLogService,
            MemberRankCommandService memberRankCommandService) {
        this.properties = properties;
        this.groupConfigService = groupConfigService;
        this.chatContextService = chatContextService;
        this.adminOpLogService = adminOpLogService;
        this.memberRankCommandService = memberRankCommandService;
    }

    public CommandHandleResult tryHandle(BotGroupMessage message, GroupConfigSnapshot config) {
        String text = message.effectiveText();
        CommandHandleResult rankCommand = memberRankCommandService.tryHandleGroup(message);
        if (rankCommand != null && rankCommand.handled()) {
            return rankCommand;
        }
        if (!text.startsWith("#")) {
            return CommandHandleResult.notCommand();
        }

        String command = firstToken(text).toLowerCase(Locale.ROOT);
        if (!isKnownCommand(command)) {
            return CommandHandleResult.notCommand();
        }

        if (!isAdmin(message.userId())) {
            return CommandHandleResult.handled("DENIED", command, "permission denied");
        }

        CommandHandleResult result = switch (command) {
            case CMD_STATUS -> status(config);
            case "#boton", CMD_BOT_RESUME -> update(
                    message, "BOT_ON", "bot_on=true", snapshot -> snapshot.withBotOn(true), "bot resumed");
            case "#botoff", CMD_BOT_QUIET -> update(
                    message, "BOT_OFF", "bot_on=false", snapshot -> snapshot.withBotOn(false), "bot quiet mode enabled");
            case "#chaton" -> update(
                    message, "CHAT_ON", "enable_chat=true", snapshot -> snapshot.withEnableChat(true), "chat on");
            case "#chatoff" -> update(
                    message,
                    "CHAT_OFF",
                    "enable_chat=false",
                    snapshot -> snapshot.withEnableChat(false).withEnableAutoJoin(false),
                    "chat and active chat off");
            case "#autochaton", CMD_ACTIVE_ON -> update(
                    message,
                    "AUTO_CHAT_ON",
                    "enable_auto_join=true",
                    snapshot -> snapshot.withEnableAutoJoin(true),
                    "active chat on");
            case "#autochatoff", CMD_ACTIVE_OFF -> update(
                    message,
                    "AUTO_CHAT_OFF",
                    "enable_auto_join=false",
                    snapshot -> snapshot.withEnableAutoJoin(false),
                    "active chat off");
            case CMD_MEME_ON -> update(
                    message, "MEME_ON", "enable_meme=true", snapshot -> snapshot.withEnableMeme(true), "meme route on");
            case CMD_MEME_OFF -> update(
                    message, "MEME_OFF", "enable_meme=false", snapshot -> snapshot.withEnableMeme(false), "meme route off");
            case CMD_KNOWLEDGE_ON -> update(
                    message,
                    "KNOWLEDGE_CONTEXT_ON",
                    "enable_knowledge_context=true",
                    snapshot -> snapshot.withEnableKnowledgeContext(true),
                    "knowledge context on");
            case CMD_KNOWLEDGE_OFF -> update(
                    message,
                    "KNOWLEDGE_CONTEXT_OFF",
                    "enable_knowledge_context=false",
                    snapshot -> snapshot.withEnableKnowledgeContext(false),
                    "knowledge context off");
            case CMD_MEME_KNOWLEDGE_ON -> update(
                    message,
                    "MEME_KNOWLEDGE_ON",
                    "enable_meme_knowledge=true",
                    snapshot -> snapshot.withEnableMemeKnowledge(true),
                    "meme knowledge on");
            case CMD_MEME_KNOWLEDGE_OFF -> update(
                    message,
                    "MEME_KNOWLEDGE_OFF",
                    "enable_meme_knowledge=false",
                    snapshot -> snapshot.withEnableMemeKnowledge(false),
                    "meme knowledge off");
            case CMD_CHAT_KNOWLEDGE_ON -> update(
                    message,
                    "PASSIVE_CHAT_KNOWLEDGE_ON",
                    "enable_passive_chat_knowledge=true",
                    snapshot -> snapshot.withEnablePassiveChatKnowledge(true),
                    "passive chat knowledge on");
            case CMD_CHAT_KNOWLEDGE_OFF -> update(
                    message,
                    "PASSIVE_CHAT_KNOWLEDGE_OFF",
                    "enable_passive_chat_knowledge=false",
                    snapshot -> snapshot.withEnablePassiveChatKnowledge(false),
                    "passive chat knowledge off");
            case CMD_ACTIVE_KNOWLEDGE_ON -> update(
                    message,
                    "ACTIVE_CHAT_KNOWLEDGE_ON",
                    "enable_active_chat_knowledge=true",
                    snapshot -> snapshot.withEnableActiveChatKnowledge(true),
                    "active chat knowledge on");
            case CMD_ACTIVE_KNOWLEDGE_OFF -> update(
                    message,
                    "ACTIVE_CHAT_KNOWLEDGE_OFF",
                    "enable_active_chat_knowledge=false",
                    snapshot -> snapshot.withEnableActiveChatKnowledge(false),
                    "active chat knowledge off");
            case "#memshort" -> update(
                    message,
                    "MEM_SHORT",
                    "memory_mode=SHORT",
                    snapshot -> snapshot.withMemoryMode(MemoryMode.SHORT),
                    "memory mode short");
            case "#memlong" -> update(
                    message,
                    "MEM_LONG",
                    "memory_mode=LONG",
                    snapshot -> snapshot.withMemoryMode(MemoryMode.LONG),
                    "memory mode long");
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

    private CommandHandleResult status(GroupConfigSnapshot config) {
        String text = """
                group status
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
                """.formatted(
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
                config.activeMaxPerDay()).strip();
        return CommandHandleResult.handled("STATUS", "group config status", text);
    }

    private CommandHandleResult clearContext(BotGroupMessage message) {
        chatContextService.clearGroupMemory(message.groupId());
        return CommandHandleResult.handled("CLEAR_CHAT_CONTEXT", "clear hot and cold context requested", "context cleared");
    }

    private CommandHandleResult setSafeWord(BotGroupMessage message, String text) {
        String value = text.length() > "#setsafeword".length()
                ? text.substring("#setsafeword".length()).strip()
                : "";
        if (value.equalsIgnoreCase("off") || value.isBlank()) {
            return update(message, "SAFE_WORD_OFF", "safe_word=null",
                    snapshot -> snapshot.withSafeWord(null), "safe word off");
        }
        return update(message, "SAFE_WORD_SET", "safe_word=" + value,
                snapshot -> snapshot.withSafeWord(value), "safe word set");
    }

    private CommandHandleResult setSafeReply(BotGroupMessage message, String text) {
        String value = text.length() > "#setsafereply".length()
                ? text.substring("#setsafereply".length()).strip()
                : "";
        if (value.isBlank()) {
            return CommandHandleResult.handled("SAFE_REPLY_REJECTED", "blank reply", "safe reply must not be blank");
        }
        return update(message, "SAFE_REPLY_SET", "safe_word_reply updated",
                snapshot -> snapshot.withSafeWordReply(value), "safe reply set");
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
            case CMD_STATUS, CMD_ACTIVE_ON, CMD_ACTIVE_OFF, CMD_MEME_ON, CMD_MEME_OFF, CMD_BOT_QUIET, CMD_BOT_RESUME,
                    CMD_KNOWLEDGE_ON, CMD_KNOWLEDGE_OFF,
                    CMD_MEME_KNOWLEDGE_ON, CMD_MEME_KNOWLEDGE_OFF,
                    CMD_CHAT_KNOWLEDGE_ON, CMD_CHAT_KNOWLEDGE_OFF,
                    CMD_ACTIVE_KNOWLEDGE_ON, CMD_ACTIVE_KNOWLEDGE_OFF,
                    "#boton", "#botoff", "#chaton", "#chatoff", "#autochaton", "#autochatoff",
                    "#memshort", "#memlong", "#clearchatctx", "#setsafeword", "#setsafereply" -> true;
            default -> false;
        };
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
