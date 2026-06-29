package com.yh.qqbot.chat.history.service.rank;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatMemberRankItem;
import com.yh.qqbot.chat.history.dto.ChatMemberRankRequest;
import com.yh.qqbot.chat.history.dto.ChatMemberRankResponse;
import com.yh.qqbot.chat.history.entity.ChatImportBatchEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatDailyEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatEntity;
import com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.config.properties.QqBotProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatMemberRankService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final ChatImportBatchMapper batchMapper;
    private final ChatMemberStatMapper memberStatMapper;
    private final ChatMemberStatDailyMapper memberStatDailyMapper;
    private final QqBotProperties properties;

    public ChatMemberRankService(
            ChatImportBatchMapper batchMapper,
            ChatMemberStatMapper memberStatMapper,
            ChatMemberStatDailyMapper memberStatDailyMapper,
            QqBotProperties properties) {
        this.batchMapper = batchMapper;
        this.memberStatMapper = memberStatMapper;
        this.memberStatDailyMapper = memberStatDailyMapper;
        this.properties = properties;
    }

    public ChatMemberRankResponse rank(ChatMemberRankRequest request) {
        if (request == null || !hasText(request.groupId())) {
            throw new InvalidChatCandidateRequestException("groupId must not be blank");
        }
        RankType rankType = RankType.parse(request.rankType());
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidChatCandidateRequestException("startDate must not be after endDate");
        }

        String groupId = request.groupId().strip();
        Long batchId = request.batchId() == null ? latestSuccessfulBatchId(groupId) : request.batchId();
        int topN = clampTopN(request.topN());
        List<ChatMemberRankItem> items = startDate != null || endDate != null
                ? rankDaily(groupId, batchId, rankType, startDate, endDate, topN)
                : rankTotal(groupId, batchId, rankType, topN);
        return new ChatMemberRankResponse(
                groupId,
                batchId,
                rankType.name(),
                rankType.label,
                startDate,
                endDate,
                topN,
                items);
    }

    private Long latestSuccessfulBatchId(String groupId) {
        ChatImportBatchEntity batch = batchMapper.selectOne(new LambdaQueryWrapper<ChatImportBatchEntity>()
                .eq(ChatImportBatchEntity::getGroupId, groupId)
                .eq(ChatImportBatchEntity::getStatus, STATUS_SUCCESS)
                .orderByDesc(ChatImportBatchEntity::getId)
                .last("LIMIT 1"));
        return batch == null ? null : batch.getId();
    }

    private List<ChatMemberRankItem> rankTotal(String groupId, Long batchId, RankType rankType, int topN) {
        LambdaQueryWrapper<ChatMemberStatEntity> wrapper = new LambdaQueryWrapper<ChatMemberStatEntity>()
                .eq(ChatMemberStatEntity::getGroupId, groupId)
                .eq(batchId != null, ChatMemberStatEntity::getBatchId, batchId);
        List<MemberRankAccumulator> accumulators = memberStatMapper.selectList(wrapper).stream()
                .map(MemberRankAccumulator::fromTotal)
                .toList();
        return rankItems(accumulators, rankType, topN);
    }

    private List<ChatMemberRankItem> rankDaily(
            String groupId,
            Long batchId,
            RankType rankType,
            LocalDate startDate,
            LocalDate endDate,
            int topN) {
        LambdaQueryWrapper<ChatMemberStatDailyEntity> wrapper = new LambdaQueryWrapper<ChatMemberStatDailyEntity>()
                .eq(ChatMemberStatDailyEntity::getGroupId, groupId)
                .eq(batchId != null, ChatMemberStatDailyEntity::getBatchId, batchId)
                .ge(startDate != null, ChatMemberStatDailyEntity::getStatDate, startDate)
                .le(endDate != null, ChatMemberStatDailyEntity::getStatDate, endDate);
        Map<String, MemberRankAccumulator> members = new HashMap<>();
        for (ChatMemberStatDailyEntity stat : memberStatDailyMapper.selectList(wrapper)) {
            String key = memberKey(stat.getSenderUid(), stat.getSenderUin(), stat.getSenderName());
            members.computeIfAbsent(key, ignored -> MemberRankAccumulator.fromDailyBase(stat)).addDaily(stat);
        }
        return rankItems(List.copyOf(members.values()), rankType, topN);
    }

    private List<ChatMemberRankItem> rankItems(
            List<MemberRankAccumulator> accumulators,
            RankType rankType,
            int topN) {
        List<MemberRankAccumulator> sorted = accumulators.stream()
                .filter(item -> rankType.score(item) > 0)
                .sorted(Comparator
                        .comparingLong((MemberRankAccumulator item) -> rankType.score(item)).reversed()
                        .thenComparing(MemberRankAccumulator::displayName, Comparator.nullsLast(String::compareTo)))
                .limit(topN)
                .toList();
        ArrayList<ChatMemberRankItem> items = new ArrayList<>();
        int rank = 1;
        for (MemberRankAccumulator member : sorted) {
            items.add(member.toItem(rank++, rankType.score(member)));
        }
        return List.copyOf(items);
    }

    private int clampTopN(Integer value) {
        int defaultTopN = Math.max(1, properties.getMemberRank().getDefaultTopN());
        int maxTopN = Math.max(defaultTopN, properties.getMemberRank().getMaxTopN());
        int requested = value == null ? defaultTopN : value;
        if (requested < 1) {
            return defaultTopN;
        }
        return Math.min(requested, maxTopN);
    }

    private static String memberKey(String uid, String uin, String name) {
        if (hasText(uid)) {
            return "uid:" + uid.strip();
        }
        if (hasText(uin)) {
            return "uin:" + uin.strip();
        }
        if (hasText(name)) {
            return "name:" + name.strip();
        }
        return "unknown";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public enum RankType {
        MESSAGE("\u53d1\u8a00", "message", "messages", "\u53d1\u8a00", "\u6d88\u606f"),
        RAW_MESSAGE("\u539f\u59cb\u6d88\u606f", "raw", "raw_message", "raw_messages", "\u539f\u59cb", "\u539f\u59cb\u6d88\u606f"),
        ACTIVE_DAYS("\u6d3b\u8dc3\u5929\u6570", "active", "active_day", "active_days", "\u6d3b\u8dc3", "\u6d3b\u8dc3\u5929\u6570"),
        MENTION("\u63d0\u5230\u522b\u4eba", "mention", "mentions", "at", "\u63d0\u5230", "\u88ab\u63d0\u5230"),
        REPLY("\u56de\u590d\u522b\u4eba", "reply", "replies", "\u56de\u590d"),
        REPLIED_BY("\u88ab\u56de\u590d", "replied", "replied_by", "\u88ab\u56de\u590d"),
        SESSION("\u53c2\u4e0e\u4f1a\u8bdd", "session", "sessions", "\u4f1a\u8bdd", "\u53c2\u4e0e\u4f1a\u8bdd");

        private final String label;
        private final List<String> aliases;

        RankType(String label, String... aliases) {
            this.label = label;
            this.aliases = List.of(aliases);
        }

        private static RankType parse(String value) {
            if (!hasText(value)) {
                return MESSAGE;
            }
            String normalized = value.strip().toLowerCase(Locale.ROOT);
            for (RankType type : values()) {
                if (type.name().equalsIgnoreCase(normalized)
                        || type.label.equalsIgnoreCase(normalized)
                        || type.aliases.contains(normalized)) {
                    return type;
                }
            }
            throw new InvalidChatCandidateRequestException("Unsupported member rank type: " + value);
        }

        private long score(MemberRankAccumulator item) {
            return switch (this) {
                case MESSAGE -> item.messageCount;
                case RAW_MESSAGE -> item.rawMessageCount;
                case ACTIVE_DAYS -> item.activeDays;
                case MENTION -> item.mentionCount;
                case REPLY -> item.replyCount;
                case REPLIED_BY -> item.repliedByCount;
                case SESSION -> item.sessionCount;
            };
        }
    }

    private static final class MemberRankAccumulator {
        private final String senderUid;
        private final String senderUin;
        private final String senderName;
        private long rawMessageCount;
        private long messageCount;
        private long activeDays;
        private long mentionCount;
        private long replyCount;
        private long repliedByCount;
        private long sessionCount;

        private MemberRankAccumulator(String senderUid, String senderUin, String senderName) {
            this.senderUid = senderUid;
            this.senderUin = senderUin;
            this.senderName = senderName;
        }

        private static MemberRankAccumulator fromTotal(ChatMemberStatEntity stat) {
            MemberRankAccumulator item = new MemberRankAccumulator(
                    stat.getSenderUid(),
                    stat.getSenderUin(),
                    stat.getSenderName());
            item.rawMessageCount = safeLong(stat.getRawMessageCount());
            item.messageCount = safeLong(stat.getMessageCount());
            item.activeDays = stat.getActiveDays() == null ? 0 : stat.getActiveDays();
            item.mentionCount = safeLong(stat.getMentionCount());
            item.replyCount = safeLong(stat.getReplyCount());
            item.repliedByCount = safeLong(stat.getRepliedByCount());
            item.sessionCount = safeLong(stat.getSessionCount());
            return item;
        }

        private static MemberRankAccumulator fromDailyBase(ChatMemberStatDailyEntity stat) {
            return new MemberRankAccumulator(stat.getSenderUid(), stat.getSenderUin(), stat.getSenderName());
        }

        private void addDaily(ChatMemberStatDailyEntity stat) {
            rawMessageCount += safeLong(stat.getRawMessageCount());
            messageCount += safeLong(stat.getMessageCount());
            activeDays += stat.getActiveDays() == null ? 0 : stat.getActiveDays();
            mentionCount += safeLong(stat.getMentionCount());
            replyCount += safeLong(stat.getReplyCount());
            repliedByCount += safeLong(stat.getRepliedByCount());
            sessionCount += safeLong(stat.getSessionCount());
        }

        private String displayName() {
            if (hasText(senderName)) {
                return senderName;
            }
            if (hasText(senderUin)) {
                return senderUin;
            }
            if (hasText(senderUid)) {
                return senderUid;
            }
            return "unknown";
        }

        private ChatMemberRankItem toItem(int rank, long score) {
            return new ChatMemberRankItem(
                    rank,
                    senderUid,
                    senderUin,
                    senderName,
                    score,
                    rawMessageCount,
                    messageCount,
                    activeDays,
                    mentionCount,
                    replyCount,
                    repliedByCount,
                    sessionCount);
        }

        private static long safeLong(Long value) {
            return value == null ? 0L : value;
        }
    }
}
