package com.yh.qqbot.chat.history.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.chat.history.dto.ChatHistoryExportData;
import com.yh.qqbot.chat.history.dto.ChatHistoryImportResponse;
import com.yh.qqbot.chat.history.dto.ChatHistoryParsedMessage;
import com.yh.qqbot.chat.history.dto.ChatMessageCleanResult;
import com.yh.qqbot.chat.history.dto.ChatSessionSplitResult;
import com.yh.qqbot.chat.history.entity.ChatCleanMessageEntity;
import com.yh.qqbot.chat.history.entity.ChatImportBatchEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageMentionEntity;
import com.yh.qqbot.chat.history.entity.ChatMessageReplyEntity;
import com.yh.qqbot.chat.history.entity.ChatRawMessageEntity;
import com.yh.qqbot.chat.history.mapper.ChatCleanMessageMapper;
import com.yh.qqbot.chat.history.mapper.ChatImportBatchMapper;
import com.yh.qqbot.chat.history.mapper.ChatMessageMentionMapper;
import com.yh.qqbot.chat.history.mapper.ChatMessageReplyMapper;
import com.yh.qqbot.chat.history.mapper.ChatRawMessageMapper;
import com.yh.qqbot.chat.history.service.cleaner.ChatMessageCleanService;
import com.yh.qqbot.chat.history.service.importer.ChatHistoryJsonParser;
import com.yh.qqbot.chat.history.service.importer.ChatHistoryPathValidator;
import com.yh.qqbot.chat.history.service.session.ChatSessionSplitService;
import com.yh.qqbot.chat.history.service.stat.ChatMemberStatService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryImportService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryImportService.class);
    private static final String STATUS_STARTED = "STARTED";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final ChatImportBatchMapper batchMapper;
    private final ChatRawMessageMapper rawMessageMapper;
    private final ChatCleanMessageMapper cleanMessageMapper;
    private final ChatMessageMentionMapper mentionMapper;
    private final ChatMessageReplyMapper replyMapper;
    private final ChatHistoryJsonParser jsonParser;
    private final ChatHistoryPathValidator pathValidator;
    private final ChatMessageCleanService cleanService;
    private final ChatSessionSplitService sessionSplitService;
    private final ChatMemberStatService memberStatService;
    private final ObjectMapper objectMapper;

    public ChatHistoryImportService(
            ChatImportBatchMapper batchMapper,
            ChatRawMessageMapper rawMessageMapper,
            ChatCleanMessageMapper cleanMessageMapper,
            ChatMessageMentionMapper mentionMapper,
            ChatMessageReplyMapper replyMapper,
            ChatHistoryJsonParser jsonParser,
            ChatHistoryPathValidator pathValidator,
            ChatMessageCleanService cleanService,
            ChatSessionSplitService sessionSplitService,
            ChatMemberStatService memberStatService,
            ObjectMapper objectMapper) {
        this.batchMapper = batchMapper;
        this.rawMessageMapper = rawMessageMapper;
        this.cleanMessageMapper = cleanMessageMapper;
        this.mentionMapper = mentionMapper;
        this.replyMapper = replyMapper;
        this.jsonParser = jsonParser;
        this.pathValidator = pathValidator;
        this.cleanService = cleanService;
        this.sessionSplitService = sessionSplitService;
        this.memberStatService = memberStatService;
        this.objectMapper = objectMapper;
    }

    public ChatHistoryImportResponse importFile(String groupId, String filePath) {
        Path source = pathValidator.resolveAllowedPath(filePath);
        String sourceHash = sha256(source);
        ChatImportBatchEntity existing = findSuccessfulBatch(groupId, sourceHash);
        if (existing != null) {
            log.info("Chat history import skipped duplicate. batchId={}, groupId={}, status={}",
                    existing.getId(), groupId, existing.getStatus());
            return response(existing, true);
        }

        ChatImportBatchEntity batch = startBatch(groupId, source, sourceHash);
        try {
            ChatHistoryExportData exportData = jsonParser.parse(source);
            updateBatchMetadata(batch, exportData);

            List<ChatRawMessageEntity> rawMessages = new ArrayList<>();
            List<ChatCleanMessageEntity> cleanMessages = new ArrayList<>();
            List<ChatMessageMentionEntity> mentions = new ArrayList<>();
            List<ChatMessageReplyEntity> replies = new ArrayList<>();
            Set<String> seenMessageIds = new LinkedHashSet<>();
            long skippedDuplicateMessages = 0L;

            int index = 0;
            for (ChatHistoryParsedMessage parsed : exportData.messages()) {
                index++;
                String messageId = stableMessageId(groupId, parsed);
                if (!seenMessageIds.add(messageId)) {
                    skippedDuplicateMessages++;
                    log.warn("Skip duplicate chat history message in same batch. batchId={}, groupId={}, messageId={}, rowIndex={}, skippedDuplicates={}",
                            batch.getId(), groupId, messageId, index, skippedDuplicateMessages);
                    continue;
                }

                ChatRawMessageEntity raw = toRawEntity(batch, groupId, parsed, messageId);
                rawMessageMapper.insert(raw);
                rawMessages.add(raw);

                cleanService.clean(parsed, raw).ifPresent(result -> {
                    cleanMessageMapper.insert(result.cleanMessage());
                    cleanMessages.add(result.cleanMessage());
                    for (ChatMessageMentionEntity mention : result.mentions()) {
                        mentionMapper.insert(mention);
                        mentions.add(mention);
                    }
                    if (result.reply() != null) {
                        replyMapper.insert(result.reply());
                        replies.add(result.reply());
                    }
                });
            }

            List<List<ChatCleanMessageEntity>> sessions = sessionSplitService.split(cleanMessages);
            ChatSessionSplitResult sessionResult = sessionSplitService.saveSessions(batch.getId(), groupId, sessions);
            long memberCount = memberStatService.calculateAndSave(
                    batch.getId(),
                    groupId,
                    rawMessages,
                    cleanMessages,
                    mentions,
                    replies,
                    sessions);

            batch.setRawCount((long) rawMessages.size());
            batch.setCleanCount((long) cleanMessages.size());
            batch.setMentionCount((long) mentions.size());
            batch.setReplyCount((long) replies.size());
            batch.setSessionCount(sessionResult.sessionCount());
            batch.setMemberCount(memberCount);
            batch.setStatus(STATUS_SUCCESS);
            batch.setUpdatedAt(LocalDateTime.now());
            batchMapper.updateById(batch);
            log.info("Chat history import completed. batchId={}, groupId={}, status={}, raw={}, clean={}, sessions={}, skippedDuplicates={}",
                    batch.getId(), groupId, batch.getStatus(), rawMessages.size(), cleanMessages.size(),
                    sessionResult.sessionCount(), skippedDuplicateMessages);
            return response(batch, false);
        } catch (Exception ex) {
            markFailed(batch, ex);
            log.warn("Chat history import failed. batchId={}, groupId={}, status={}, error={}",
                    batch.getId(), groupId, STATUS_FAILED, shortErrorMessage(ex), ex);
            throw new IllegalStateException("chat history import failed", ex);
        }
    }

    private ChatImportBatchEntity startBatch(String groupId, Path source, String sourceHash) {
        ChatImportBatchEntity batch = new ChatImportBatchEntity();
        batch.setGroupId(groupId);
        batch.setSourceFile(relativePath(source));
        batch.setSourceHash(sourceHash);
        batch.setTotalMessages(0L);
        batch.setRawCount(0L);
        batch.setCleanCount(0L);
        batch.setMentionCount(0L);
        batch.setReplyCount(0L);
        batch.setSessionCount(0L);
        batch.setMemberCount(0L);
        batch.setStatus(STATUS_STARTED);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.insert(batch);
        return batch;
    }

    private void updateBatchMetadata(ChatImportBatchEntity batch, ChatHistoryExportData exportData) {
        batch.setChatName(exportData.chatName());
        batch.setExporterName(exportData.exporterName());
        batch.setExporterVersion(exportData.exporterVersion());
        batch.setStartTime(exportData.startTime());
        batch.setEndTime(exportData.endTime());
        batch.setTotalMessages((long) exportData.messages().size());
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    private ChatRawMessageEntity toRawEntity(
            ChatImportBatchEntity batch,
            String groupId,
            ChatHistoryParsedMessage parsed,
            String messageId) throws JsonProcessingException {
        ChatRawMessageEntity raw = new ChatRawMessageEntity();
        raw.setBatchId(batch.getId());
        raw.setGroupId(groupId);
        raw.setMessageId(messageId);
        raw.setSeq(parsed.seq());
        raw.setMessageTime(parsed.messageTime());
        raw.setSenderUid(parsed.senderUid());
        raw.setSenderUin(parsed.senderUin());
        raw.setSenderName(parsed.senderName());
        raw.setSenderGroupCard(parsed.senderGroupCard());
        raw.setMessageType(parsed.messageType());
        raw.setRawText(parsed.rawText());
        raw.setSystemFlag(parsed.system());
        raw.setRecalledFlag(parsed.recalled());
        raw.setHasResource(parsed.hasResource());
        raw.setElementTypes(String.join(",", parsed.elementTypes()));
        raw.setRawJson(objectMapper.writeValueAsString(parsed.rawJson()));
        return raw;
    }

    private ChatImportBatchEntity findSuccessfulBatch(String groupId, String sourceHash) {
        return batchMapper.selectOne(new LambdaQueryWrapper<ChatImportBatchEntity>()
                .eq(ChatImportBatchEntity::getGroupId, groupId)
                .eq(ChatImportBatchEntity::getSourceHash, sourceHash)
                .eq(ChatImportBatchEntity::getStatus, STATUS_SUCCESS)
                .last("LIMIT 1"));
    }

    private void markFailed(ChatImportBatchEntity batch, Exception ex) {
        batch.setStatus(STATUS_FAILED);
        batch.setErrorMessage(limit(shortErrorMessage(ex), 1000));
        batch.setUpdatedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    private ChatHistoryImportResponse response(ChatImportBatchEntity batch, boolean duplicateImport) {
        return new ChatHistoryImportResponse(
                batch.getId(),
                safeLong(batch.getRawCount()),
                safeLong(batch.getCleanCount()),
                safeLong(batch.getMentionCount()),
                safeLong(batch.getReplyCount()),
                safeLong(batch.getSessionCount()),
                safeLong(batch.getMemberCount()),
                batch.getStatus(),
                duplicateImport
        );
    }

    private String sha256(Path source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(source);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInput.read(buffer) != -1) {
                    // DigestInputStream updates the digest while reading.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("failed to hash chat history file", ex);
        }
    }

    private String relativePath(Path source) {
        Path root = Paths.get("").toAbsolutePath().normalize();
        return root.relativize(source).toString().replace('\\', '/');
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String stableMessageId(String groupId, ChatHistoryParsedMessage parsed) {
        if (parsed.messageId() != null && !parsed.messageId().isBlank()) {
            return parsed.messageId().strip();
        }
        String fingerprint = String.join("|",
                nullToEmpty(groupId),
                nullToEmpty(parsed.seq()),
                nullToEmpty(parsed.messageTime()),
                nullToEmpty(parsed.senderUid()),
                nullToEmpty(parsed.senderUin()),
                nullToEmpty(parsed.senderName()),
                nullToEmpty(parsed.messageType()),
                nullToEmpty(parsed.rawText()),
                parsed.rawJson() == null ? "" : parsed.rawJson().toString());
        return "fallback-" + sha256Text(fingerprint).substring(0, 32);
    }

    private String sha256Text(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("failed to hash chat history message", ex);
        }
    }

    private String shortErrorMessage(Exception ex) {
        String message = ex.getMessage();
        return ex.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
