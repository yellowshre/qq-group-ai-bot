package com.yh.qqbot.chat.history.service.candidate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yh.qqbot.chat.history.dto.ChatCandidateGenerateResponse;
import com.yh.qqbot.chat.history.dto.ChatCandidateStatus;
import com.yh.qqbot.chat.history.dto.ChatKnowledgeCandidateType;
import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberCandidateEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import com.yh.qqbot.chat.history.entity.ChatSessionEntity;
import com.yh.qqbot.chat.history.entity.ChatSessionMessageEntity;
import com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberCandidateMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper;
import com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper;
import com.yh.qqbot.chat.history.mapper.ChatSessionMapper;
import com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatKnowledgeCandidateGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ChatKnowledgeCandidateGenerationService.class);
    private static final DateTimeFormatter SESSION_TITLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern HAS_LETTER_OR_DIGIT = Pattern.compile("[\\p{IsHan}\\p{L}\\p{N}]");
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final Pattern ONLY_PUNCT_OR_SYMBOL = Pattern.compile("[\\p{P}\\p{S}\\s]+");

    private final ChatCleanMessageMapper cleanMessageMapper;
    private final ChatMessageReplyMapper replyMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatSessionMessageMapper sessionMessageMapper;
    private final ChatMemberStatMapper memberStatMapper;
    private final ChatKnowledgeCandidateMapper knowledgeCandidateMapper;
    private final ChatMemberCandidateMapper memberCandidateMapper;

    public ChatKnowledgeCandidateGenerationService(
            ChatCleanMessageMapper cleanMessageMapper,
            ChatMessageReplyMapper replyMapper,
            ChatSessionMapper sessionMapper,
            ChatSessionMessageMapper sessionMessageMapper,
            ChatMemberStatMapper memberStatMapper,
            ChatKnowledgeCandidateMapper knowledgeCandidateMapper,
            ChatMemberCandidateMapper memberCandidateMapper) {
        this.cleanMessageMapper = cleanMessageMapper;
        this.replyMapper = replyMapper;
        this.sessionMapper = sessionMapper;
        this.sessionMessageMapper = sessionMessageMapper;
        this.memberStatMapper = memberStatMapper;
        this.knowledgeCandidateMapper = knowledgeCandidateMapper;
        this.memberCandidateMapper = memberCandidateMapper;
    }

    public ChatCandidateGenerateResponse generate(Long batchId, String groupId) {
        List<ChatCleanMessageEntity> cleanMessages = cleanMessageMapper.selectList(
                new LambdaQueryWrapper<ChatCleanMessageEntity>()
                        .eq(ChatCleanMessageEntity::getBatchId, batchId)
                        .eq(ChatCleanMessageEntity::getGroupId, groupId));
        List<ChatKnowledgeCandidateEntity> knowledgeCandidates = new ArrayList<>();
        knowledgeCandidates.addAll(buildPhraseCandidates(batchId, groupId, cleanMessages));
        knowledgeCandidates.addAll(buildReplyPatternCandidates(batchId, groupId, cleanMessages));
        knowledgeCandidates.addAll(buildTopicCandidates(batchId, groupId));

        long insertedKnowledge = 0;
        for (ChatKnowledgeCandidateEntity candidate : knowledgeCandidates) {
            if (!knowledgeCandidateExists(candidate.getBatchId(), candidate.getGroupId(),
                    candidate.getCandidateType(), candidate.getContent())) {
                knowledgeCandidateMapper.insert(candidate);
                insertedKnowledge++;
            }
        }

        long insertedMembers = 0;
        for (ChatMemberCandidateEntity candidate : buildMemberCandidates(batchId, groupId)) {
            if (!memberCandidateExists(candidate)) {
                memberCandidateMapper.insert(candidate);
                insertedMembers++;
            }
        }

        log.info("Chat knowledge candidate generation completed. batchId={}, groupId={}, knowledgeCandidates={}, memberCandidates={}, status=SUCCESS",
                batchId, groupId, insertedKnowledge, insertedMembers);
        return new ChatCandidateGenerateResponse(batchId, insertedKnowledge, insertedMembers, "SUCCESS");
    }

    public List<ChatKnowledgeCandidateEntity> buildPhraseCandidates(
            Long batchId,
            String groupId,
            List<ChatCleanMessageEntity> cleanMessages) {
        Map<String, TextStats> stats = new LinkedHashMap<>();
        for (ChatCleanMessageEntity message : cleanMessages) {
            String text = normalizeText(message.getCleanText());
            if (!isPhraseCandidateText(text)) {
                continue;
            }
            stats.computeIfAbsent(text, TextStats::new).add(message);
        }

        return stats.values().stream()
                .filter(stat -> stat.hitCount() >= 3)
                .filter(stat -> stat.memberCount() >= 2)
                .sorted(Comparator.comparing(TextStats::hitCount).reversed()
                        .thenComparing(TextStats::memberCount, Comparator.reverseOrder()))
                .map(stat -> phraseCandidate(batchId, groupId, stat))
                .toList();
    }

    public List<ChatKnowledgeCandidateEntity> buildReplyPatternCandidates(
            Long batchId,
            String groupId,
            List<ChatCleanMessageEntity> cleanMessages) {
        List<ChatCleanMessageEntity> replyMessages = cleanMessages.stream()
                .filter(message -> Boolean.TRUE.equals(message.getIsReply()))
                .filter(message -> {
                    String text = normalizeText(message.getCleanText());
                    return text.length() >= 2 && text.length() <= 40 && HAS_LETTER_OR_DIGIT.matcher(text).find();
                })
                .toList();
        if (replyMessages.isEmpty()) {
            return List.of();
        }

        Set<String> messageIds = replyMessages.stream()
                .map(ChatCleanMessageEntity::getMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ChatMessageReplyEntity> repliesByMessageId = replyMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageReplyEntity>()
                                .eq(ChatMessageReplyEntity::getGroupId, groupId)
                                .in(!messageIds.isEmpty(), ChatMessageReplyEntity::getMessageId, messageIds))
                .stream()
                .collect(Collectors.toMap(ChatMessageReplyEntity::getMessageId, reply -> reply, (first, second) -> first));

        Map<String, TextStats> stats = new LinkedHashMap<>();
        for (ChatCleanMessageEntity message : replyMessages) {
            String text = normalizeText(message.getCleanText());
            stats.computeIfAbsent(text, TextStats::new).add(message);
        }

        return stats.values().stream()
                .filter(stat -> stat.hitCount() >= 2)
                .map(stat -> replyCandidate(batchId, groupId, stat, repliesByMessageId))
                .toList();
    }

    public List<ChatKnowledgeCandidateEntity> buildTopicCandidates(Long batchId, String groupId) {
        List<ChatSessionEntity> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getBatchId, batchId)
                        .eq(ChatSessionEntity::getGroupId, groupId)
                        .ge(ChatSessionEntity::getMessageCount, 10)
                        .ge(ChatSessionEntity::getMemberCount, 3));
        List<ChatKnowledgeCandidateEntity> candidates = new ArrayList<>();
        for (ChatSessionEntity session : sessions) {
            List<ChatCleanMessageEntity> messages = cleanMessagesForSession(session.getId());
            if (messages.isEmpty()) {
                continue;
            }
            ChatKnowledgeCandidateEntity candidate = baseKnowledgeCandidate(
                    batchId,
                    groupId,
                    ChatKnowledgeCandidateType.TOPIC,
                    "会话片段 " + (session.getStartTime() == null
                            ? "unknown"
                            : SESSION_TITLE_TIME.format(session.getStartTime())),
                    limit(joinTexts(messages, 500), 500));
            candidate.setSourceSessionId(session.getId());
            candidate.setSourceMessageIds(sourceMessageIds(messages));
            candidate.setEvidenceText(limit(joinTexts(messages, 300), 300));
            candidate.setHitCount((long) messages.size());
            candidate.setMemberCount((long) Math.max(0, session.getMemberCount()));
            candidate.setConfidence(BigDecimal.valueOf(Math.min(1.0, messages.size() / 20.0))
                    .setScale(4, RoundingMode.HALF_UP));
            candidates.add(candidate);
        }
        return candidates;
    }

    public List<ChatMemberCandidateEntity> buildMemberCandidates(Long batchId, String groupId) {
        return memberStatMapper.selectList(new LambdaQueryWrapper<ChatMemberStatEntity>()
                        .eq(ChatMemberStatEntity::getBatchId, batchId)
                        .eq(ChatMemberStatEntity::getGroupId, groupId))
                .stream()
                .map(this::memberCandidate)
                .filter(candidate -> safeLong(candidate.getMessageCount()) >= 10 || candidate.getScore() > 0)
                .sorted(Comparator.comparing(ChatMemberCandidateEntity::getScore).reversed())
                .limit(10)
                .toList();
    }

    public long score(ChatMemberStatEntity stat) {
        return safeLong(stat.getMessageCount())
                + safeInt(stat.getActiveDays()) * 5L
                + safeLong(stat.getMentionCount()) * 2L
                + safeLong(stat.getReplyCount()) * 2L
                + safeLong(stat.getRepliedByCount()) * 2L
                + safeLong(stat.getSessionCount()) * 3L;
    }

    public boolean isPhraseCandidateText(String text) {
        String normalized = normalizeText(text);
        return normalized.length() >= 2
                && normalized.length() <= 30
                && !ONLY_DIGITS.matcher(normalized).matches()
                && !ONLY_PUNCT_OR_SYMBOL.matcher(normalized).matches()
                && HAS_LETTER_OR_DIGIT.matcher(normalized).find();
    }

    private ChatKnowledgeCandidateEntity phraseCandidate(Long batchId, String groupId, TextStats stat) {
        ChatKnowledgeCandidateEntity candidate = baseKnowledgeCandidate(
                batchId,
                groupId,
                ChatKnowledgeCandidateType.PHRASE,
                limit(stat.text(), 20),
                stat.text());
        candidate.setSourceMessageIds(String.join(",", stat.messageIds()));
        candidate.setEvidenceText(limit(stat.text(), 200));
        candidate.setHitCount((long) stat.hitCount());
        candidate.setMemberCount((long) stat.memberCount());
        candidate.setConfidence(confidence(stat.hitCount(), stat.memberCount()));
        return candidate;
    }

    private ChatKnowledgeCandidateEntity replyCandidate(
            Long batchId,
            String groupId,
            TextStats stat,
            Map<String, ChatMessageReplyEntity> repliesByMessageId) {
        ChatKnowledgeCandidateEntity candidate = baseKnowledgeCandidate(
                batchId,
                groupId,
                ChatKnowledgeCandidateType.REPLY_PATTERN,
                limit(stat.text(), 20),
                stat.text());
        candidate.setSourceMessageIds(String.join(",", stat.messageIds()));
        candidate.setEvidenceText(replyEvidence(stat, repliesByMessageId));
        candidate.setHitCount((long) stat.hitCount());
        candidate.setMemberCount((long) stat.memberCount());
        candidate.setConfidence(confidence(stat.hitCount(), stat.memberCount()));
        return candidate;
    }

    private String replyEvidence(TextStats stat, Map<String, ChatMessageReplyEntity> repliesByMessageId) {
        for (String messageId : stat.messageIds()) {
            ChatMessageReplyEntity reply = repliesByMessageId.get(messageId);
            if (reply != null && reply.getReplyContent() != null && !reply.getReplyContent().isBlank()) {
                return limit("被回复: " + reply.getReplyContent().strip() + " / 回复: " + stat.text(), 300);
            }
        }
        return limit("回复: " + stat.text(), 300);
    }

    private ChatMemberCandidateEntity memberCandidate(ChatMemberStatEntity stat) {
        ChatMemberCandidateEntity candidate = new ChatMemberCandidateEntity();
        candidate.setBatchId(stat.getBatchId());
        candidate.setGroupId(stat.getGroupId());
        candidate.setSenderUid(stat.getSenderUid());
        candidate.setSenderUin(stat.getSenderUin());
        candidate.setSenderName(stat.getSenderName());
        candidate.setMessageCount(safeLong(stat.getMessageCount()));
        candidate.setRawMessageCount(safeLong(stat.getRawMessageCount()));
        candidate.setActiveDays(safeInt(stat.getActiveDays()));
        candidate.setMentionCount(safeLong(stat.getMentionCount()));
        candidate.setReplyCount(safeLong(stat.getReplyCount()));
        candidate.setRepliedByCount(safeLong(stat.getRepliedByCount()));
        candidate.setSessionCount(safeLong(stat.getSessionCount()));
        candidate.setScore(score(stat));
        candidate.setCandidateReason(memberReason(candidate));
        candidate.setStatus(ChatCandidateStatus.PENDING.name());
        return candidate;
    }

    private String memberReason(ChatMemberCandidateEntity candidate) {
        return "发言数=" + candidate.getMessageCount()
                + ", 活跃天数=" + candidate.getActiveDays()
                + ", @次数=" + candidate.getMentionCount()
                + ", 回复数=" + candidate.getReplyCount()
                + ", 被回复数=" + candidate.getRepliedByCount()
                + ", 会话数=" + candidate.getSessionCount();
    }

    private ChatKnowledgeCandidateEntity baseKnowledgeCandidate(
            Long batchId,
            String groupId,
            ChatKnowledgeCandidateType type,
            String title,
            String content) {
        ChatKnowledgeCandidateEntity candidate = new ChatKnowledgeCandidateEntity();
        candidate.setBatchId(batchId);
        candidate.setGroupId(groupId);
        candidate.setCandidateType(type.name());
        candidate.setTitle(title);
        candidate.setContent(content);
        candidate.setHitCount(0L);
        candidate.setMemberCount(0L);
        candidate.setConfidence(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        candidate.setStatus(ChatCandidateStatus.PENDING.name());
        return candidate;
    }

    private List<ChatCleanMessageEntity> cleanMessagesForSession(Long sessionId) {
        List<ChatSessionMessageEntity> relations = sessionMessageMapper.selectList(
                new LambdaQueryWrapper<ChatSessionMessageEntity>()
                        .eq(ChatSessionMessageEntity::getSessionId, sessionId)
                        .orderByAsc(ChatSessionMessageEntity::getMessageOrder));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> orderById = new HashMap<>();
        List<Long> cleanIds = new ArrayList<>();
        for (ChatSessionMessageEntity relation : relations) {
            cleanIds.add(relation.getCleanMessageId());
            orderById.put(relation.getCleanMessageId(), relation.getMessageOrder());
        }
        return cleanMessageMapper.selectBatchIds(cleanIds).stream()
                .sorted(Comparator.comparing(message -> orderById.getOrDefault(message.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private boolean knowledgeCandidateExists(Long batchId, String groupId, String candidateType, String content) {
        return knowledgeCandidateMapper.selectCount(new LambdaQueryWrapper<ChatKnowledgeCandidateEntity>()
                .eq(ChatKnowledgeCandidateEntity::getBatchId, batchId)
                .eq(ChatKnowledgeCandidateEntity::getGroupId, groupId)
                .eq(ChatKnowledgeCandidateEntity::getCandidateType, candidateType)
                .eq(ChatKnowledgeCandidateEntity::getContent, content)) > 0;
    }

    private boolean memberCandidateExists(ChatMemberCandidateEntity candidate) {
        LambdaQueryWrapper<ChatMemberCandidateEntity> wrapper = new LambdaQueryWrapper<ChatMemberCandidateEntity>()
                .eq(ChatMemberCandidateEntity::getBatchId, candidate.getBatchId())
                .eq(ChatMemberCandidateEntity::getGroupId, candidate.getGroupId());
        if (candidate.getSenderUid() != null && !candidate.getSenderUid().isBlank()) {
            wrapper.eq(ChatMemberCandidateEntity::getSenderUid, candidate.getSenderUid());
        } else if (candidate.getSenderUin() != null && !candidate.getSenderUin().isBlank()) {
            wrapper.eq(ChatMemberCandidateEntity::getSenderUin, candidate.getSenderUin());
        } else {
            wrapper.eq(ChatMemberCandidateEntity::getSenderName, candidate.getSenderName());
        }
        return memberCandidateMapper.selectCount(wrapper) > 0;
    }

    private BigDecimal confidence(int hitCount, int memberCount) {
        double score = Math.min(1.0, hitCount / 10.0 + memberCount / 10.0);
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private String sourceMessageIds(List<ChatCleanMessageEntity> messages) {
        return messages.stream()
                .map(ChatCleanMessageEntity::getMessageId)
                .filter(Objects::nonNull)
                .limit(20)
                .collect(Collectors.joining(","));
    }

    private String joinTexts(List<ChatCleanMessageEntity> messages, int maxLength) {
        StringBuilder builder = new StringBuilder();
        for (ChatCleanMessageEntity message : messages) {
            String text = normalizeText(message.getCleanText());
            if (text.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(text);
            if (builder.length() >= maxLength) {
                break;
            }
        }
        return limit(builder.toString(), maxLength);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private String limit(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class TextStats {
        private final String text;
        private int hitCount;
        private final Set<String> memberKeys = new HashSet<>();
        private final List<String> messageIds = new ArrayList<>();

        private TextStats(String text) {
            this.text = text;
        }

        private void add(ChatCleanMessageEntity message) {
            hitCount++;
            memberKeys.add(firstNonBlank(message.getSenderUid(), message.getSenderUin(), message.getSenderName()));
            if (message.getMessageId() != null && !message.getMessageId().isBlank()) {
                messageIds.add(message.getMessageId());
            }
        }

        private String text() {
            return text;
        }

        private int hitCount() {
            return hitCount;
        }

        private int memberCount() {
            memberKeys.remove(null);
            return memberKeys.size();
        }

        private List<String> messageIds() {
            return messageIds;
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }
}
