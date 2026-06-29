package com.yh.qqbot.service.command;

import com.yh.qqbot.chat.history.dto.ChatMemberRankItem;
import com.yh.qqbot.chat.history.dto.ChatMemberRankRequest;
import com.yh.qqbot.chat.history.dto.ChatMemberRankResponse;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.chat.history.service.rank.ChatMemberRankService;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.CommandHandleResult;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MemberRankCommandService {

    private static final String OP_MEMBER_RANK = "MEMBER_RANK";
    private static final String OP_MEMBER_RANK_DISABLED = "MEMBER_RANK_DISABLED";
    private static final String OP_MEMBER_RANK_DENIED = "MEMBER_RANK_DENIED";
    private static final String OP_MEMBER_RANK_INVALID = "MEMBER_RANK_INVALID";

    private final QqBotProperties properties;
    private final ChatMemberRankService rankService;

    public MemberRankCommandService(QqBotProperties properties, ChatMemberRankService rankService) {
        this.properties = properties;
        this.rankService = rankService;
    }

    public CommandHandleResult tryHandleGroup(BotGroupMessage message) {
        String text = message == null ? "" : message.effectiveText();
        String prefix = commandPrefix();
        if (!hasText(text) || !text.strip().startsWith(prefix)) {
            return CommandHandleResult.notCommand();
        }
        if (!properties.getMemberRank().isEnabled() || !properties.getMemberRank().isGroupCommandEnabled()) {
            return silent(OP_MEMBER_RANK_DISABLED, "member rank group command disabled");
        }
        if (properties.getMemberRank().isAdminOnly() && !isAdmin(message.userId())) {
            return silent(OP_MEMBER_RANK_DENIED, "member rank admin only");
        }
        return rankCommand(message.groupId(), text.strip().substring(prefix.length()).strip());
    }

    public CommandHandleResult tryHandlePrivate(String groupId, String operationText) {
        if (!hasText(operationText)) {
            return CommandHandleResult.notCommand();
        }
        String operation = operationText.strip();
        String prefix = commandPrefix();
        String privateCommandWord = privateCommandWord();
        String body;
        if (operation.startsWith(prefix)) {
            body = operation.substring(prefix.length()).strip();
        } else if (operation.startsWith(privateCommandWord)) {
            body = operation.substring(privateCommandWord.length()).strip();
        } else {
            return CommandHandleResult.notCommand();
        }
        if (!properties.getMemberRank().isEnabled() || !properties.getMemberRank().isPrivateCommandEnabled()) {
            return silent(OP_MEMBER_RANK_DISABLED, "member rank private command disabled");
        }
        return rankCommand(groupId, body);
    }

    private CommandHandleResult rankCommand(String groupId, String body) {
        try {
            ParsedRankCommand parsed = parseBody(body);
            ChatMemberRankResponse response = rankService.rank(new ChatMemberRankRequest(
                    groupId,
                    null,
                    parsed.rankType(),
                    parsed.startDate(),
                    parsed.endDate(),
                    parsed.topN()));
            return CommandHandleResult.handled(OP_MEMBER_RANK, "member rank query", formatResponse(response));
        } catch (InvalidChatCandidateRequestException | IllegalArgumentException ex) {
            return CommandHandleResult.handled(OP_MEMBER_RANK_INVALID, ex.getMessage(), rankUsage());
        }
    }

    private ParsedRankCommand parseBody(String body) {
        String rankType = "MESSAGE";
        LocalDate startDate = null;
        LocalDate endDate = null;
        Integer topN = null;
        boolean rankTypeSet = false;
        for (String token : splitTokens(body)) {
            if (!hasText(token)) {
                continue;
            }
            Integer parsedTopN = parseTopN(token);
            if (parsedTopN != null) {
                topN = parsedTopN;
                continue;
            }
            LocalDate parsedDate = parseDate(token);
            if (parsedDate != null) {
                if (startDate == null) {
                    startDate = parsedDate;
                } else {
                    endDate = parsedDate;
                }
                continue;
            }
            if (!rankTypeSet) {
                rankType = token.strip();
                rankTypeSet = true;
            }
        }
        if (startDate != null && endDate == null) {
            endDate = startDate;
        }
        return new ParsedRankCommand(rankType, startDate, endDate, topN);
    }

    private List<String> splitTokens(String body) {
        if (!hasText(body)) {
            return List.of();
        }
        return List.of(body.strip().split("\\s+"));
    }

    private Integer parseTopN(String token) {
        String normalized = token.strip().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("top")) {
            return parsePositiveInt(normalized.substring(3));
        }
        if (normalized.startsWith("\u524d")) {
            return parsePositiveInt(normalized.substring(1));
        }
        return null;
    }

    private Integer parsePositiveInt(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String token) {
        try {
            return LocalDate.parse(token.strip());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String formatResponse(ChatMemberRankResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("\u7fa4 ").append(response.groupId()).append(' ')
                .append(response.rankTypeLabel()).append("\u6392\u884c Top").append(response.topN()).append('\n');
        if (response.startDate() != null || response.endDate() != null) {
            builder.append("\u8303\u56f4\uff1a")
                    .append(response.startDate() == null ? "" : response.startDate())
                    .append(" \u81f3 ")
                    .append(response.endDate() == null ? "" : response.endDate())
                    .append('\n');
        } else {
            builder.append("\u8303\u56f4\uff1a\u5f53\u524d\u6279\u6b21\u5168\u91cf\u7edf\u8ba1\n");
        }
        if (response.items().isEmpty()) {
            builder.append("\u6682\u65e0\u6392\u884c\u6570\u636e");
            return builder.toString();
        }
        for (ChatMemberRankItem item : response.items()) {
            builder.append(item.rank()).append(". ")
                    .append(displayName(item))
                    .append(' ')
                    .append(item.score())
                    .append('\n');
        }
        return builder.toString().strip();
    }

    private String displayName(ChatMemberRankItem item) {
        if (hasText(item.senderName())) {
            return item.senderName();
        }
        if (hasText(item.senderUin())) {
            return item.senderUin();
        }
        if (hasText(item.senderUid())) {
            return item.senderUid();
        }
        return "unknown";
    }

    private String rankUsage() {
        return "\u6392\u884c\u6307\u4ee4\u683c\u5f0f\uff1a#\u6392\u884c \u53d1\u8a00 top5\uff0c\u6216 #\u7fa4 123456 \u6392\u884c \u53d1\u8a00 2026-06-01 2026-06-29 top10";
    }

    private CommandHandleResult silent(String operation, String detail) {
        return new CommandHandleResult(true, null, operation, detail);
    }

    private String commandPrefix() {
        String prefix = properties.getMemberRank().getCommandPrefix();
        return hasText(prefix) ? prefix.strip() : "#\u6392\u884c";
    }

    private String privateCommandWord() {
        String prefix = commandPrefix();
        return prefix.startsWith("#") ? prefix.substring(1) : prefix;
    }

    private boolean isAdmin(String userId) {
        if (!hasText(userId)) {
            return false;
        }
        return splitValues(properties.getAdmins()).contains(userId.strip());
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedRankCommand(
            String rankType,
            LocalDate startDate,
            LocalDate endDate,
            Integer topN) {
    }
}
