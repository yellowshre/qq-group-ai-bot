package com.yh.qqbot.chat.history.service.vector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.chat.history.dto.FormalKnowledgeStatus;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingGenerateResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingStatus;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingTargetType;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchResult;
import com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity;
import com.yh.qqbot.chat.history.entity.ChatKnowledgeEmbeddingEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberProfileEntity;
import com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper;
import com.yh.qqbot.chat.history.mapper.ChatKnowledgeEmbeddingMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEmbeddingService.class);
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    private final ChatGroupKnowledgeMapper groupKnowledgeMapper;
    private final ChatMemberProfileMapper memberProfileMapper;
    private final ChatKnowledgeEmbeddingMapper embeddingMapper;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public KnowledgeEmbeddingService(
            ChatGroupKnowledgeMapper groupKnowledgeMapper,
            ChatMemberProfileMapper memberProfileMapper,
            ChatKnowledgeEmbeddingMapper embeddingMapper,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper) {
        this.groupKnowledgeMapper = groupKnowledgeMapper;
        this.memberProfileMapper = memberProfileMapper;
        this.embeddingMapper = embeddingMapper;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KnowledgeEmbeddingGenerateResponse generate(KnowledgeEmbeddingGenerateRequest request) {
        if (request == null || !hasText(request.groupId())) {
            throw new InvalidChatCandidateRequestException("groupId is required");
        }
        if (!embeddingService.enabled()) {
            log.warn("Skip knowledge embedding generation because embedding is disabled.");
            return new KnowledgeEmbeddingGenerateResponse(0, 0, 0, "DISABLED");
        }
        Set<KnowledgeEmbeddingTargetType> targetTypes = parseTargetTypes(request.targetTypes());
        boolean regenerate = Boolean.TRUE.equals(request.regenerate());
        Counter counter = new Counter();
        if (targetTypes.contains(KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE)) {
            for (ChatGroupKnowledgeEntity knowledge : enabledKnowledge(request.groupId())) {
                generateOne(KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE, knowledge.getId(), request.groupId(),
                        buildKnowledgeEmbeddingText(knowledge), regenerate, counter);
            }
        }
        if (targetTypes.contains(KnowledgeEmbeddingTargetType.MEMBER_PROFILE)) {
            for (ChatMemberProfileEntity profile : enabledMemberProfiles(request.groupId())) {
                generateOne(KnowledgeEmbeddingTargetType.MEMBER_PROFILE, profile.getId(), request.groupId(),
                        buildMemberEmbeddingText(profile), regenerate, counter);
            }
        }
        return new KnowledgeEmbeddingGenerateResponse(counter.embedded, counter.skipped, counter.failed, "SUCCESS");
    }

    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        if (request == null || !hasText(request.groupId())) {
            throw new InvalidChatCandidateRequestException("groupId is required");
        }
        if (!hasText(request.query())) {
            throw new InvalidChatCandidateRequestException("query is required");
        }
        if (!embeddingService.enabled()) {
            log.warn("Skip knowledge vector search because embedding is disabled.");
            return new KnowledgeSearchResponse(request.query(), List.of());
        }
        List<Double> queryVector;
        try {
            queryVector = embeddingService.embed(request.query());
        } catch (Exception ex) {
            log.warn("Knowledge vector search query embedding failed. groupId={}, model={}",
                    request.groupId(), embeddingService.model(), ex);
            return new KnowledgeSearchResponse(request.query(), List.of());
        }

        Set<KnowledgeEmbeddingTargetType> targetTypes = parseTargetTypes(request.targetTypes());
        List<KnowledgeSearchCandidate> candidates = new ArrayList<>();
        for (ChatKnowledgeEmbeddingEntity embedding : successEmbeddings(request.groupId(), targetTypes)) {
            SearchPayload payload = resolveSearchPayload(embedding);
            if (payload == null) {
                continue;
            }
            List<Double> vector = parseVector(embedding);
            if (vector.isEmpty() || vector.size() != queryVector.size()) {
                log.warn("Skip vector search candidate because dimension mismatch. targetType={}, targetId={}, model={}, queryDim={}, targetDim={}",
                        embedding.getTargetType(), embedding.getTargetId(), embedding.getEmbeddingModel(),
                        queryVector.size(), vector.size());
                continue;
            }
            candidates.add(new KnowledgeSearchCandidate(
                    embedding.getTargetType(),
                    embedding.getTargetId(),
                    payload.title(),
                    payload.content(),
                    vector));
        }
        return new KnowledgeSearchResponse(
                request.query(),
                rankSearchCandidates(candidates, queryVector, topK(request.topK())));
    }

    public List<KnowledgeSearchResult> rankSearchCandidates(
            List<KnowledgeSearchCandidate> candidates,
            List<Double> queryVector,
            int topK) {
        if (candidates == null || candidates.isEmpty() || queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> new KnowledgeSearchResult(
                        candidate.targetType(),
                        candidate.targetId(),
                        VectorSimilarityUtils.round4(VectorSimilarityUtils.cosineSimilarity(queryVector, candidate.vector())),
                        candidate.title(),
                        candidate.content()))
                .sorted(Comparator.comparingDouble(KnowledgeSearchResult::score).reversed()
                        .thenComparing(KnowledgeSearchResult::targetType)
                        .thenComparing(KnowledgeSearchResult::targetId))
                .limit(Math.max(1, topK))
                .toList();
    }

    public String buildKnowledgeEmbeddingText(ChatGroupKnowledgeEntity knowledge) {
        return "Type: " + safe(knowledge.getKnowledgeType())
                + "\nTitle: " + safe(knowledge.getTitle())
                + "\nContent: " + safe(knowledge.getContent())
                + "\nEvidence: " + safe(knowledge.getEvidenceText());
    }

    public String buildMemberEmbeddingText(ChatMemberProfileEntity profile) {
        return "Member: " + safe(displayName(profile))
                + "\nProfile: " + safe(profile.getProfileText());
    }

    private void generateOne(
            KnowledgeEmbeddingTargetType targetType,
            Long targetId,
            String groupId,
            String text,
            boolean regenerate,
            Counter counter) {
        String model = embeddingService.model();
        String hash = hash(model + "\n" + text);
        if (!regenerate && hasSuccessEmbedding(groupId, targetType, targetId, model, hash)) {
            counter.skipped++;
            return;
        }
        try {
            List<Double> vector = embeddingService.embed(text);
            insertEmbedding(groupId, targetType, targetId, model, text, vector, hash, null);
            counter.embedded++;
            log.info("Knowledge embedding generated. targetType={}, targetId={}, model={}, dimension={}, status={}",
                    targetType.name(), targetId, model, vector.size(), KnowledgeEmbeddingStatus.SUCCESS.name());
        } catch (Exception ex) {
            insertEmbedding(groupId, targetType, targetId, model, null, List.of(), hash, limit(ex.getMessage(), 1000));
            counter.failed++;
            log.warn("Knowledge embedding failed. targetType={}, targetId={}, model={}, status={}",
                    targetType.name(), targetId, model, KnowledgeEmbeddingStatus.FAILED.name(), ex);
        }
    }

    private void insertEmbedding(
            String groupId,
            KnowledgeEmbeddingTargetType targetType,
            Long targetId,
            String model,
            String text,
            List<Double> vector,
            String hash,
            String errorMessage) {
        ChatKnowledgeEmbeddingEntity entity = new ChatKnowledgeEmbeddingEntity();
        entity.setGroupId(groupId);
        entity.setTargetType(targetType.name());
        entity.setTargetId(targetId);
        entity.setEmbeddingModel(model);
        entity.setEmbeddingDim(vector == null ? 0 : vector.size());
        entity.setEmbeddingText(text);
        entity.setEmbeddingVector(vector == null || vector.isEmpty() ? null : toJson(vector));
        entity.setEmbeddingHash(hash);
        entity.setStatus(errorMessage == null ? KnowledgeEmbeddingStatus.SUCCESS.name() : KnowledgeEmbeddingStatus.FAILED.name());
        entity.setErrorMessage(errorMessage);
        embeddingMapper.insert(entity);
    }

    private String toJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception ex) {
            throw new IllegalStateException("embedding vector serialize failed", ex);
        }
    }

    private boolean hasSuccessEmbedding(
            String groupId,
            KnowledgeEmbeddingTargetType targetType,
            Long targetId,
            String model,
            String hash) {
        return embeddingMapper.selectOne(new LambdaQueryWrapper<ChatKnowledgeEmbeddingEntity>()
                .eq(ChatKnowledgeEmbeddingEntity::getGroupId, groupId)
                .eq(ChatKnowledgeEmbeddingEntity::getTargetType, targetType.name())
                .eq(ChatKnowledgeEmbeddingEntity::getTargetId, targetId)
                .eq(ChatKnowledgeEmbeddingEntity::getEmbeddingModel, model)
                .eq(ChatKnowledgeEmbeddingEntity::getEmbeddingHash, hash)
                .eq(ChatKnowledgeEmbeddingEntity::getStatus, KnowledgeEmbeddingStatus.SUCCESS.name())
                .last("LIMIT 1")) != null;
    }

    private List<ChatGroupKnowledgeEntity> enabledKnowledge(String groupId) {
        return groupKnowledgeMapper.selectList(new LambdaQueryWrapper<ChatGroupKnowledgeEntity>()
                .eq(ChatGroupKnowledgeEntity::getGroupId, groupId)
                .eq(ChatGroupKnowledgeEntity::getEnabled, true)
                .eq(ChatGroupKnowledgeEntity::getStatus, FormalKnowledgeStatus.ACTIVE.name())
                .orderByAsc(ChatGroupKnowledgeEntity::getId));
    }

    private List<ChatMemberProfileEntity> enabledMemberProfiles(String groupId) {
        return memberProfileMapper.selectList(new LambdaQueryWrapper<ChatMemberProfileEntity>()
                .eq(ChatMemberProfileEntity::getGroupId, groupId)
                .eq(ChatMemberProfileEntity::getEnabled, true)
                .eq(ChatMemberProfileEntity::getStatus, FormalKnowledgeStatus.ACTIVE.name())
                .orderByAsc(ChatMemberProfileEntity::getId));
    }

    private List<ChatKnowledgeEmbeddingEntity> successEmbeddings(
            String groupId,
            Set<KnowledgeEmbeddingTargetType> targetTypes) {
        List<String> targetTypeNames = targetTypes.stream().map(Enum::name).toList();
        return embeddingMapper.selectList(new LambdaQueryWrapper<ChatKnowledgeEmbeddingEntity>()
                .eq(ChatKnowledgeEmbeddingEntity::getGroupId, groupId)
                .eq(ChatKnowledgeEmbeddingEntity::getEmbeddingModel, embeddingService.model())
                .eq(ChatKnowledgeEmbeddingEntity::getStatus, KnowledgeEmbeddingStatus.SUCCESS.name())
                .in(ChatKnowledgeEmbeddingEntity::getTargetType, targetTypeNames)
                .orderByDesc(ChatKnowledgeEmbeddingEntity::getUpdatedAt)
                .orderByAsc(ChatKnowledgeEmbeddingEntity::getId));
    }

    private SearchPayload resolveSearchPayload(ChatKnowledgeEmbeddingEntity embedding) {
        if (KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name().equals(embedding.getTargetType())) {
            ChatGroupKnowledgeEntity knowledge = groupKnowledgeMapper.selectById(embedding.getTargetId());
            if (knowledge == null
                    || !Boolean.TRUE.equals(knowledge.getEnabled())
                    || !FormalKnowledgeStatus.ACTIVE.name().equals(knowledge.getStatus())) {
                return null;
            }
            return new SearchPayload(knowledge.getTitle(), knowledge.getContent());
        }
        if (KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name().equals(embedding.getTargetType())) {
            ChatMemberProfileEntity profile = memberProfileMapper.selectById(embedding.getTargetId());
            if (profile == null
                    || !Boolean.TRUE.equals(profile.getEnabled())
                    || !FormalKnowledgeStatus.ACTIVE.name().equals(profile.getStatus())) {
                return null;
            }
            return new SearchPayload(displayName(profile), profile.getProfileText());
        }
        return null;
    }

    private List<Double> parseVector(ChatKnowledgeEmbeddingEntity embedding) {
        if (!hasText(embedding.getEmbeddingVector())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(embedding.getEmbeddingVector(), DOUBLE_LIST);
        } catch (Exception ex) {
            log.warn("Knowledge embedding vector parse failed. targetType={}, targetId={}, model={}",
                    embedding.getTargetType(), embedding.getTargetId(), embedding.getEmbeddingModel(), ex);
            return List.of();
        }
    }

    private Set<KnowledgeEmbeddingTargetType> parseTargetTypes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EnumSet.allOf(KnowledgeEmbeddingTargetType.class);
        }
        EnumSet<KnowledgeEmbeddingTargetType> targetTypes = EnumSet.noneOf(KnowledgeEmbeddingTargetType.class);
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            try {
                targetTypes.add(KnowledgeEmbeddingTargetType.valueOf(value.strip()));
            } catch (IllegalArgumentException ex) {
                throw new InvalidChatCandidateRequestException("targetTypes is invalid");
            }
        }
        return targetTypes.isEmpty() ? EnumSet.allOf(KnowledgeEmbeddingTargetType.class) : targetTypes;
    }

    private int topK(Integer value) {
        if (value == null) {
            return 5;
        }
        return Math.min(20, Math.max(1, value));
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String displayName(ChatMemberProfileEntity profile) {
        if (hasText(profile.getSenderName())) {
            return profile.getSenderName();
        }
        if (hasText(profile.getSenderUin())) {
            return profile.getSenderUin();
        }
        if (hasText(profile.getSenderUid())) {
            return profile.getSenderUid();
        }
        return "unknown";
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record KnowledgeSearchCandidate(
            String targetType,
            Long targetId,
            String title,
            String content,
            List<Double> vector
    ) {
    }

    private record SearchPayload(String title, String content) {
    }

    private static final class Counter {
        private long embedded;
        private long skipped;
        private long failed;
    }
}
