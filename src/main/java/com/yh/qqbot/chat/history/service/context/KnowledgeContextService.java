package com.yh.qqbot.chat.history.service.context;

import com.yh.qqbot.chat.history.dto.DifyContextSimulateRequest;
import com.yh.qqbot.chat.history.dto.DifyContextSimulateResponse;
import com.yh.qqbot.chat.history.dto.FormalKnowledgeStatus;
import com.yh.qqbot.chat.history.dto.KnowledgeContextItem;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeEmbeddingTargetType;
import com.yh.qqbot.chat.history.dto.KnowledgeRouteType;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeSearchResult;
import com.yh.qqbot.chat.history.entity.ChatGroupKnowledgeEntity;
import com.yh.qqbot.chat.history.entity.ChatMemberProfileEntity;
import com.yh.qqbot.chat.history.mapper.ChatGroupKnowledgeMapper;
import com.yh.qqbot.chat.history.mapper.ChatMemberProfileMapper;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.chat.history.service.vector.KnowledgeEmbeddingService;
import com.yh.qqbot.config.properties.QqBotProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeContextService {

    private static final String NO_KNOWLEDGE_USED = "NO_KNOWLEDGE_USED";
    private static final Set<String> MEME_TYPES = Set.of("PHRASE", "MEME", "MEME_SCENE");
    private static final Set<String> PASSIVE_TYPES = Set.of("PHRASE", "TOPIC");
    private static final Set<String> ACTIVE_TYPES = Set.of("PHRASE", "TOPIC");

    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final ChatGroupKnowledgeMapper groupKnowledgeMapper;
    private final ChatMemberProfileMapper memberProfileMapper;
    private final QqBotProperties properties;

    public KnowledgeContextService(
            KnowledgeEmbeddingService knowledgeEmbeddingService,
            ChatGroupKnowledgeMapper groupKnowledgeMapper,
            ChatMemberProfileMapper memberProfileMapper,
            QqBotProperties properties) {
        this.knowledgeEmbeddingService = knowledgeEmbeddingService;
        this.groupKnowledgeMapper = groupKnowledgeMapper;
        this.memberProfileMapper = memberProfileMapper;
        this.properties = properties;
    }

    public KnowledgeContextPreviewResponse preview(KnowledgeContextPreviewRequest request) {
        KnowledgeRouteType routeType = parseRouteType(request == null ? null : request.routeType());
        String groupId = requiredText(request == null ? null : request.groupId(), "groupId");
        String messageText = requiredText(request == null ? null : request.messageText(), "messageText");
        KnowledgeContextBuildResult result = buildContext(
                groupId,
                messageText,
                request.senderUid(),
                routeType,
                request.topK());
        return new KnowledgeContextPreviewResponse(
                routeType.name(),
                messageText,
                result.knowledgeUsed(),
                result.knowledgeContext(),
                result.items(),
                result.knowledgeUsed() ? null : NO_KNOWLEDGE_USED);
    }

    public DifyContextSimulateResponse simulateDifyInputs(DifyContextSimulateRequest request) {
        KnowledgeRouteType routeType = parseRouteType(request == null ? null : request.routeType());
        String groupId = requiredText(request == null ? null : request.groupId(), "groupId");
        String messageText = requiredText(request == null ? null : request.messageText(), "messageText");
        KnowledgeContextBuildResult result = buildContext(
                groupId,
                messageText,
                request.senderUid(),
                routeType,
                request.topK());
        Map<String, Object> inputs = buildDifyInputs(request, routeType, result.knowledgeContext());
        return new DifyContextSimulateResponse(
                routeType.name(),
                workflowName(routeType),
                messageText,
                result.knowledgeUsed(),
                result.knowledgeContext(),
                result.items(),
                inputs);
    }

    public KnowledgeContextBuildResult buildContext(
            String groupId,
            String messageText,
            String senderUid,
            KnowledgeRouteType routeType,
            Integer topK) {
        String safeGroupId = requiredText(groupId, "groupId");
        String safeMessageText = requiredText(messageText, "messageText");
        KnowledgeRouteType safeRouteType = routeType == null ? KnowledgeRouteType.PASSIVE_CHAT : routeType;
        int limit = itemLimit(topK);
        List<String> targetTypes = targetTypesFor(safeRouteType);
        int searchTopK = Math.min(
                Math.max(1, properties.getKnowledge().getContext().getMaxSearchCandidates()),
                Math.max(limit * 4, limit));
        List<KnowledgeSearchResult> searchResults = knowledgeEmbeddingService.search(
                new KnowledgeSearchRequest(safeGroupId, safeMessageText, searchTopK, targetTypes)).results();
        List<KnowledgeContextItem> items = selectContextItems(searchResults, safeRouteType, limit);
        String context = formatKnowledgeContext(items);
        return new KnowledgeContextBuildResult(!items.isEmpty() && !context.isBlank(), context, items);
    }

    public Map<String, Object> buildDifyInputs(
            DifyContextSimulateRequest request,
            KnowledgeRouteType routeType,
            String knowledgeContext) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("text", request.messageText() == null ? "" : request.messageText());
        inputs.put("groupId", request.groupId() == null ? "" : request.groupId());
        inputs.put("userId", request.senderUid() == null ? "" : request.senderUid());
        inputs.put("knowledgeContext", knowledgeContext == null ? "" : knowledgeContext);
        if (routeType == KnowledgeRouteType.PASSIVE_CHAT || routeType == KnowledgeRouteType.ACTIVE_CHAT) {
            inputs.put("botName", hasText(request.botName()) ? request.botName().strip() : botName());
            inputs.put("persona", hasText(request.persona()) ? request.persona().strip() : persona());
            inputs.put("recentMessages", hasText(request.recentMessages()) ? request.recentMessages().strip() : "");
        }
        if (routeType == KnowledgeRouteType.ACTIVE_CHAT) {
            inputs.put("activeReason", hasText(request.activeReason()) ? request.activeReason().strip() : "");
            inputs.put("riskHint", hasText(request.riskHint()) ? request.riskHint().strip() : "Use low-risk group knowledge only.");
        }
        return inputs;
    }

    private List<KnowledgeContextItem> selectContextItems(
            List<KnowledgeSearchResult> searchResults,
            KnowledgeRouteType routeType,
            int limit) {
        if (searchResults == null || searchResults.isEmpty()) {
            return List.of();
        }
        List<KnowledgeContextItem> items = new ArrayList<>();
        int memberCount = 0;
        for (KnowledgeSearchResult result : searchResults) {
            if (result.score() < properties.getKnowledge().getContext().getMinScore()) {
                continue;
            }
            if (KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name().equals(result.targetType())) {
                KnowledgeContextItem item = resolveKnowledgeItem(result, routeType);
                if (item != null) {
                    items.add(item);
                }
            } else if (KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name().equals(result.targetType())
                    && routeType == KnowledgeRouteType.PASSIVE_CHAT
                    && memberCount < properties.getKnowledge().getContext().getMemberProfileLimit()) {
                KnowledgeContextItem item = resolveMemberProfileItem(result);
                if (item != null) {
                    items.add(item);
                    memberCount++;
                }
            }
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private KnowledgeContextItem resolveKnowledgeItem(KnowledgeSearchResult result, KnowledgeRouteType routeType) {
        ChatGroupKnowledgeEntity knowledge = groupKnowledgeMapper.selectById(result.targetId());
        if (knowledge == null
                || !Boolean.TRUE.equals(knowledge.getEnabled())
                || !FormalKnowledgeStatus.ACTIVE.name().equals(knowledge.getStatus())
                || !allowedKnowledgeTypes(routeType).contains(knowledge.getKnowledgeType())) {
            return null;
        }
        return new KnowledgeContextItem(
                KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name(),
                knowledge.getId(),
                knowledge.getKnowledgeType(),
                limit(knowledge.getTitle(), 80),
                limit(knowledge.getContent(), properties.getKnowledge().getContext().getMaxItemContentLength()),
                result.score(),
                usageHint(knowledge.getKnowledgeType(), routeType));
    }

    private KnowledgeContextItem resolveMemberProfileItem(KnowledgeSearchResult result) {
        ChatMemberProfileEntity profile = memberProfileMapper.selectById(result.targetId());
        if (profile == null
                || !Boolean.TRUE.equals(profile.getEnabled())
                || !FormalKnowledgeStatus.ACTIVE.name().equals(profile.getStatus())) {
            return null;
        }
        return new KnowledgeContextItem(
                KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name(),
                profile.getId(),
                KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name(),
                limit(displayName(profile), 80),
                limit(profile.getProfileText(), properties.getKnowledge().getContext().getMaxItemContentLength()),
                result.score(),
                "Use only as lightweight passive-chat member context.");
    }

    private String formatKnowledgeContext(List<KnowledgeContextItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        int maxLength = Math.max(1, properties.getKnowledge().getContext().getMaxLength());
        StringBuilder builder = new StringBuilder("Reviewed group knowledge only:\n");
        int index = 1;
        for (KnowledgeContextItem item : items) {
            String line = index + ". [" + item.type() + "] "
                    + safe(item.title()) + ": " + safe(item.content())
                    + " Hint: " + safe(item.usageHint())
                    + " Score: " + item.score()
                    + "\n";
            if (builder.length() + line.length() > maxLength) {
                break;
            }
            builder.append(line);
            index++;
        }
        String context = builder.toString().strip();
        return context.length() <= maxLength ? context : context.substring(0, maxLength);
    }

    private List<String> targetTypesFor(KnowledgeRouteType routeType) {
        if (routeType == KnowledgeRouteType.PASSIVE_CHAT) {
            return List.of(
                    KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name(),
                    KnowledgeEmbeddingTargetType.MEMBER_PROFILE.name());
        }
        return List.of(KnowledgeEmbeddingTargetType.GROUP_KNOWLEDGE.name());
    }

    private Set<String> allowedKnowledgeTypes(KnowledgeRouteType routeType) {
        return switch (routeType) {
            case MEME -> MEME_TYPES;
            case PASSIVE_CHAT -> PASSIVE_TYPES;
            case ACTIVE_CHAT -> ACTIVE_TYPES;
        };
    }

    private String usageHint(String knowledgeType, KnowledgeRouteType routeType) {
        if ("MEME".equals(knowledgeType) || "MEME_SCENE".equals(knowledgeType)) {
            return "Use to help classify meme scene only.";
        }
        if ("TOPIC".equals(knowledgeType)) {
            return routeType == KnowledgeRouteType.ACTIVE_CHAT
                    ? "Use only as low-risk topic background."
                    : "Use as group topic background.";
        }
        return "Use to understand reviewed group phrase meaning.";
    }

    private int itemLimit(Integer topK) {
        int configuredMax = Math.max(1, properties.getKnowledge().getContext().getMaxItems());
        if (topK == null) {
            return configuredMax;
        }
        return Math.min(configuredMax, Math.max(1, topK));
    }

    private KnowledgeRouteType parseRouteType(String routeType) {
        if (!hasText(routeType)) {
            throw new InvalidChatCandidateRequestException("routeType is required");
        }
        try {
            return KnowledgeRouteType.valueOf(routeType.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidChatCandidateRequestException("routeType is invalid");
        }
    }

    private String workflowName(KnowledgeRouteType routeType) {
        QqBotProperties.Dify dify = properties.getDify();
        return switch (routeType) {
            case MEME -> dify.getSceneWorkflowId();
            case PASSIVE_CHAT -> dify.getPassiveChatWorkflowId();
            case ACTIVE_CHAT -> dify.getActiveWorkflowId();
        };
    }

    private String botName() {
        String configured = properties.getIdentity().getDisplayName();
        if (hasText(configured)) {
            return configured.strip();
        }
        return "bot";
    }

    private String persona() {
        String configured = properties.getIdentity().getDefaultPersona();
        if (hasText(configured)) {
            return configured.strip();
        }
        return properties.getDefaultPersona();
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

    private String requiredText(String value, String name) {
        if (!hasText(value)) {
            throw new InvalidChatCandidateRequestException(name + " is required");
        }
        return value.strip();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String stripped = value.strip();
        int safeMax = Math.max(1, maxLength);
        return stripped.length() <= safeMax ? stripped : stripped.substring(0, safeMax);
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record KnowledgeContextBuildResult(
            boolean knowledgeUsed,
            String knowledgeContext,
            List<KnowledgeContextItem> items
    ) {
    }
}
