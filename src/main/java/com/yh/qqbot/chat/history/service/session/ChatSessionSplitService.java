package com.yh.qqbot.chat.history.service.session;

import com.yh.qqbot.chat.history.dto.ChatSessionSplitResult;
import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatSessionEntity;
import com.yh.qqbot.chat.history.entity.ChatSessionMessageEntity;
import com.yh.qqbot.chat.history.mapper.ChatSessionMapper;
import com.yh.qqbot.chat.history.mapper.ChatSessionMessageMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionSplitService {

    private static final Duration SESSION_GAP = Duration.ofMinutes(15);

    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionMessageMapper chatSessionMessageMapper;

    public ChatSessionSplitService(
            ChatSessionMapper chatSessionMapper,
            ChatSessionMessageMapper chatSessionMessageMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatSessionMessageMapper = chatSessionMessageMapper;
    }

    public ChatSessionSplitResult splitAndSave(Long batchId, String groupId, List<ChatCleanMessageEntity> cleanMessages) {
        List<List<ChatCleanMessageEntity>> sessions = split(cleanMessages);
        return saveSessions(batchId, groupId, sessions);
    }

    public ChatSessionSplitResult saveSessions(
            Long batchId,
            String groupId,
            List<List<ChatCleanMessageEntity>> sessions) {
        long relationCount = 0;
        int sessionNo = 1;
        for (List<ChatCleanMessageEntity> messages : sessions) {
            ChatSessionEntity session = buildSession(batchId, groupId, sessionNo++, messages);
            chatSessionMapper.insert(session);
            int order = 1;
            for (ChatCleanMessageEntity message : messages) {
                ChatSessionMessageEntity relation = new ChatSessionMessageEntity();
                relation.setSessionId(session.getId());
                relation.setCleanMessageId(message.getId());
                relation.setMessageOrder(order++);
                chatSessionMessageMapper.insert(relation);
                relationCount++;
            }
        }
        return new ChatSessionSplitResult(sessions.size(), relationCount);
    }

    public List<List<ChatCleanMessageEntity>> split(List<ChatCleanMessageEntity> cleanMessages) {
        if (cleanMessages == null || cleanMessages.isEmpty()) {
            return List.of();
        }
        List<ChatCleanMessageEntity> sorted = cleanMessages.stream()
                .sorted(Comparator
                        .comparing(ChatCleanMessageEntity::getMessageTime,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ChatCleanMessageEntity::getSeq,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();

        List<List<ChatCleanMessageEntity>> sessions = new ArrayList<>();
        List<ChatCleanMessageEntity> current = new ArrayList<>();
        ChatCleanMessageEntity previous = null;
        for (ChatCleanMessageEntity message : sorted) {
            if (previous != null && shouldStartNewSession(previous, message)) {
                sessions.add(current);
                current = new ArrayList<>();
            }
            current.add(message);
            previous = message;
        }
        if (!current.isEmpty()) {
            sessions.add(current);
        }
        return sessions;
    }

    private boolean shouldStartNewSession(ChatCleanMessageEntity previous, ChatCleanMessageEntity current) {
        if (previous.getMessageTime() == null || current.getMessageTime() == null) {
            return false;
        }
        return Duration.between(previous.getMessageTime(), current.getMessageTime()).compareTo(SESSION_GAP) > 0;
    }

    private ChatSessionEntity buildSession(
            Long batchId,
            String groupId,
            int sessionNo,
            List<ChatCleanMessageEntity> messages) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setBatchId(batchId);
        session.setGroupId(groupId);
        session.setSessionNo(sessionNo);
        session.setStartTime(messages.get(0).getMessageTime());
        session.setEndTime(messages.get(messages.size() - 1).getMessageTime());
        session.setMessageCount(messages.size());
        session.setMemberCount(memberKeys(messages).size());
        return session;
    }

    private Set<String> memberKeys(List<ChatCleanMessageEntity> messages) {
        return messages.stream()
                .map(message -> firstNonBlank(message.getSenderUid(), message.getSenderUin(), message.getSenderName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
