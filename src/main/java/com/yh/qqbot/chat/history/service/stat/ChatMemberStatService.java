package com.yh.qqbot.chat.history.service.stat;

import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatDailyEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageMentionEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import com.yh.qqbot.chat.history.entity.ChatRawMessageEntity;
import com.yh.qqbot.chat.history.mapper.ChatMemberStatDailyMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberStatMapper;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ChatMemberStatService {

    private final ChatMemberStatMapper chatMemberStatMapper;
    private final ChatMemberStatDailyMapper chatMemberStatDailyMapper;

    public ChatMemberStatService(
            ChatMemberStatMapper chatMemberStatMapper,
            ChatMemberStatDailyMapper chatMemberStatDailyMapper) {
        this.chatMemberStatMapper = chatMemberStatMapper;
        this.chatMemberStatDailyMapper = chatMemberStatDailyMapper;
    }

    public long calculateAndSave(
            Long batchId,
            String groupId,
            List<ChatRawMessageEntity> rawMessages,
            List<ChatCleanMessageEntity> cleanMessages,
            List<ChatMessageMentionEntity> mentions,
            List<ChatMessageReplyEntity> replies,
            List<List<ChatCleanMessageEntity>> sessions) {
        Map<String, MemberAccumulator> members = new HashMap<>();
        Map<String, MemberAccumulator> dailyMembers = new HashMap<>();
        Map<Long, String> rawIdToMemberKey = new HashMap<>();
        Map<Long, LocalDate> rawIdToDate = new HashMap<>();

        for (ChatRawMessageEntity raw : rawMessages) {
            MemberAccumulator member = member(members, raw.getSenderUid(), raw.getSenderUin(), raw.getSenderName());
            member.rawMessageCount++;
            rawIdToMemberKey.put(raw.getId(), member.key);
            LocalDate date = statDate(raw);
            rawIdToDate.put(raw.getId(), date);
            MemberAccumulator daily = dailyMember(dailyMembers, date, raw.getSenderUid(), raw.getSenderUin(), raw.getSenderName());
            if (daily != null) {
                daily.rawMessageCount++;
            }
        }

        for (ChatCleanMessageEntity clean : cleanMessages) {
            MemberAccumulator member = member(members, clean.getSenderUid(), clean.getSenderUin(), clean.getSenderName());
            member.messageCount++;
            if (clean.getMessageTime() != null) {
                member.activeDays.add(clean.getMessageTime().toLocalDate());
            }
            LocalDate date = statDate(clean);
            MemberAccumulator daily = dailyMember(dailyMembers, date, clean.getSenderUid(), clean.getSenderUin(), clean.getSenderName());
            if (daily != null) {
                daily.messageCount++;
                daily.activeDays.add(date);
            }
        }

        for (ChatMessageMentionEntity mention : mentions) {
            MemberAccumulator sender = findByUidOrCreate(members, mention.getSenderUid());
            if (sender != null) {
                sender.mentionCount++;
            }
            LocalDate date = rawIdToDate.get(mention.getRawMessageId());
            MemberAccumulator daily = findDailyByUidOrCreate(dailyMembers, date, mention.getSenderUid());
            if (daily != null) {
                daily.mentionCount++;
            }
        }

        Map<String, MemberAccumulator> byUin = byUin(members.values());
        Map<String, MemberAccumulator> byName = byName(members.values());
        for (ChatMessageReplyEntity reply : replies) {
            String senderKey = rawIdToMemberKey.get(reply.getRawMessageId());
            MemberAccumulator sender = senderKey == null ? null : members.get(senderKey);
            LocalDate date = rawIdToDate.get(reply.getRawMessageId());
            if (sender != null) {
                sender.replyCount++;
                MemberAccumulator daily = dailyMember(dailyMembers, date,
                        sender.senderUid, sender.senderUin, sender.senderName);
                if (daily != null) {
                    daily.replyCount++;
                }
            }
            MemberAccumulator replied = firstNonNull(
                    byUin.get(reply.getReplySenderUin()),
                    byName.get(reply.getReplySenderName()));
            if (replied != null) {
                replied.repliedByCount++;
                MemberAccumulator daily = dailyMember(dailyMembers, date,
                        replied.senderUid, replied.senderUin, replied.senderName);
                if (daily != null) {
                    daily.repliedByCount++;
                }
            }
        }

        for (List<ChatCleanMessageEntity> session : sessions) {
            Set<String> touchedMembers = new HashSet<>();
            Set<String> touchedDailyMembers = new HashSet<>();
            for (ChatCleanMessageEntity message : session) {
                MemberAccumulator member = member(members,
                        message.getSenderUid(),
                        message.getSenderUin(),
                        message.getSenderName());
                touchedMembers.add(member.key);
                LocalDate date = statDate(message);
                MemberAccumulator daily = dailyMember(dailyMembers, date,
                        message.getSenderUid(),
                        message.getSenderUin(),
                        message.getSenderName());
                if (daily != null) {
                    touchedDailyMembers.add(daily.key);
                }
            }
            for (String memberKey : touchedMembers) {
                members.get(memberKey).sessionCount++;
            }
            for (String dailyMemberKey : touchedDailyMembers) {
                dailyMembers.get(dailyMemberKey).sessionCount++;
            }
        }

        for (MemberAccumulator member : members.values()) {
            chatMemberStatMapper.insert(member.toEntity(batchId, groupId));
        }
        for (MemberAccumulator member : dailyMembers.values()) {
            chatMemberStatDailyMapper.insert(member.toDailyEntity(batchId, groupId));
        }
        return members.size();
    }

    private MemberAccumulator findByUidOrCreate(Map<String, MemberAccumulator> members, String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return member(members, uid, null, null);
    }

    private MemberAccumulator findDailyByUidOrCreate(
            Map<String, MemberAccumulator> members,
            LocalDate date,
            String uid) {
        if (date == null || uid == null || uid.isBlank()) {
            return null;
        }
        return dailyMember(members, date, uid, null, null);
    }

    private MemberAccumulator member(Map<String, MemberAccumulator> members, String uid, String uin, String name) {
        String key = memberKey(uid, uin, name);
        return members.computeIfAbsent(key, ignored -> new MemberAccumulator(key, null, uid, uin, name));
    }

    private MemberAccumulator dailyMember(
            Map<String, MemberAccumulator> members,
            LocalDate date,
            String uid,
            String uin,
            String name) {
        if (date == null) {
            return null;
        }
        String key = dailyMemberKey(date, uid, uin, name);
        return members.computeIfAbsent(key, ignored -> new MemberAccumulator(key, date, uid, uin, name));
    }

    private String memberKey(String uid, String uin, String name) {
        String value = firstNonBlank(uid, uin, name);
        return value == null ? "unknown" : value;
    }

    private String dailyMemberKey(LocalDate date, String uid, String uin, String name) {
        return date + ":" + memberKey(uid, uin, name);
    }

    private LocalDate statDate(ChatRawMessageEntity message) {
        return message == null || message.getMessageTime() == null ? null : message.getMessageTime().toLocalDate();
    }

    private LocalDate statDate(ChatCleanMessageEntity message) {
        return message == null || message.getMessageTime() == null ? null : message.getMessageTime().toLocalDate();
    }

    private Map<String, MemberAccumulator> byUin(Collection<MemberAccumulator> members) {
        Map<String, MemberAccumulator> byUin = new HashMap<>();
        for (MemberAccumulator member : members) {
            if (member.senderUin != null && !member.senderUin.isBlank()) {
                byUin.put(member.senderUin, member);
            }
        }
        return byUin;
    }

    private Map<String, MemberAccumulator> byName(Collection<MemberAccumulator> members) {
        Map<String, MemberAccumulator> byName = new HashMap<>();
        for (MemberAccumulator member : members) {
            if (member.senderName != null && !member.senderName.isBlank()) {
                byName.put(member.senderName, member);
            }
        }
        return byName;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static final class MemberAccumulator {
        private final String key;
        private final LocalDate statDate;
        private final String senderUid;
        private final String senderUin;
        private final String senderName;
        private long rawMessageCount;
        private long messageCount;
        private long mentionCount;
        private long replyCount;
        private long repliedByCount;
        private long sessionCount;
        private final Set<LocalDate> activeDays = new HashSet<>();

        private MemberAccumulator(String key, LocalDate statDate, String senderUid, String senderUin, String senderName) {
            this.key = Objects.requireNonNull(key);
            this.statDate = statDate;
            this.senderUid = senderUid;
            this.senderUin = senderUin;
            this.senderName = senderName;
        }

        private ChatMemberStatEntity toEntity(Long batchId, String groupId) {
            ChatMemberStatEntity entity = new ChatMemberStatEntity();
            entity.setBatchId(batchId);
            entity.setGroupId(groupId);
            entity.setSenderUid(senderUid);
            entity.setSenderUin(senderUin);
            entity.setSenderName(senderName);
            entity.setRawMessageCount(rawMessageCount);
            entity.setMessageCount(messageCount);
            entity.setActiveDays(activeDays.size());
            entity.setMentionCount(mentionCount);
            entity.setReplyCount(replyCount);
            entity.setRepliedByCount(repliedByCount);
            entity.setSessionCount(sessionCount);
            return entity;
        }

        private ChatMemberStatDailyEntity toDailyEntity(Long batchId, String groupId) {
            ChatMemberStatDailyEntity entity = new ChatMemberStatDailyEntity();
            entity.setBatchId(batchId);
            entity.setGroupId(groupId);
            entity.setStatDate(statDate);
            entity.setSenderUid(senderUid);
            entity.setSenderUin(senderUin);
            entity.setSenderName(senderName);
            entity.setRawMessageCount(rawMessageCount);
            entity.setMessageCount(messageCount);
            entity.setActiveDays(activeDays.isEmpty() ? 0 : 1);
            entity.setMentionCount(mentionCount);
            entity.setReplyCount(replyCount);
            entity.setRepliedByCount(repliedByCount);
            entity.setSessionCount(sessionCount);
            return entity;
        }
    }
}
