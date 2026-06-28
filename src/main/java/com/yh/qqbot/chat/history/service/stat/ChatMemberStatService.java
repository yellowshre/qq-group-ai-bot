package com.yh.qqbot.chat.history.service.stat;

import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberStatEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageMentionEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import com.yh.qqbot.chat.history.entity.ChatRawMessageEntity;
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

    public ChatMemberStatService(ChatMemberStatMapper chatMemberStatMapper) {
        this.chatMemberStatMapper = chatMemberStatMapper;
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
        Map<Long, String> rawIdToMemberKey = new HashMap<>();

        for (ChatRawMessageEntity raw : rawMessages) {
            MemberAccumulator member = member(members, raw.getSenderUid(), raw.getSenderUin(), raw.getSenderName());
            member.rawMessageCount++;
            rawIdToMemberKey.put(raw.getId(), member.key);
        }

        for (ChatCleanMessageEntity clean : cleanMessages) {
            MemberAccumulator member = member(members, clean.getSenderUid(), clean.getSenderUin(), clean.getSenderName());
            member.messageCount++;
            if (clean.getMessageTime() != null) {
                member.activeDays.add(clean.getMessageTime().toLocalDate());
            }
        }

        for (ChatMessageMentionEntity mention : mentions) {
            MemberAccumulator sender = findByUidOrCreate(members, mention.getSenderUid());
            if (sender != null) {
                sender.mentionCount++;
            }
        }

        Map<String, MemberAccumulator> byUin = byUin(members.values());
        Map<String, MemberAccumulator> byName = byName(members.values());
        for (ChatMessageReplyEntity reply : replies) {
            String senderKey = rawIdToMemberKey.get(reply.getRawMessageId());
            MemberAccumulator sender = senderKey == null ? null : members.get(senderKey);
            if (sender != null) {
                sender.replyCount++;
            }
            MemberAccumulator replied = firstNonNull(
                    byUin.get(reply.getReplySenderUin()),
                    byName.get(reply.getReplySenderName()));
            if (replied != null) {
                replied.repliedByCount++;
            }
        }

        for (List<ChatCleanMessageEntity> session : sessions) {
            Set<String> touchedMembers = new HashSet<>();
            for (ChatCleanMessageEntity message : session) {
                MemberAccumulator member = member(members,
                        message.getSenderUid(),
                        message.getSenderUin(),
                        message.getSenderName());
                touchedMembers.add(member.key);
            }
            for (String memberKey : touchedMembers) {
                members.get(memberKey).sessionCount++;
            }
        }

        for (MemberAccumulator member : members.values()) {
            chatMemberStatMapper.insert(member.toEntity(batchId, groupId));
        }
        return members.size();
    }

    private MemberAccumulator findByUidOrCreate(Map<String, MemberAccumulator> members, String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return member(members, uid, null, null);
    }

    private MemberAccumulator member(Map<String, MemberAccumulator> members, String uid, String uin, String name) {
        String key = memberKey(uid, uin, name);
        return members.computeIfAbsent(key, ignored -> new MemberAccumulator(key, uid, uin, name));
    }

    private String memberKey(String uid, String uin, String name) {
        String value = firstNonBlank(uid, uin, name);
        return value == null ? "unknown" : value;
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

        private MemberAccumulator(String key, String senderUid, String senderUin, String senderName) {
            this.key = Objects.requireNonNull(key);
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
    }
}
