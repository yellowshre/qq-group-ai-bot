package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.DifyContextSimulateRequest;
import com.yh.qqbot.chat.history.dto.DifyContextSimulateResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeContextItem;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeRoutePreviewRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeRoutePreviewResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeRouteType;
import com.yh.qqbot.chat.history.dto.RouteKnowledgePreview;
import com.yh.qqbot.chat.history.service.context.KnowledgeContextService;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.service.config.GroupConfigService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatKnowledgeContextController {

    private final KnowledgeContextService knowledgeContextService;
    private final GroupConfigService groupConfigService;

    public ChatKnowledgeContextController(
            KnowledgeContextService knowledgeContextService,
            GroupConfigService groupConfigService) {
        this.knowledgeContextService = knowledgeContextService;
        this.groupConfigService = groupConfigService;
    }

    @PostMapping("/knowledge/context/preview")
    public KnowledgeContextPreviewResponse preview(
            @Valid @RequestBody KnowledgeContextPreviewRequest request) {
        return knowledgeContextService.preview(request);
    }

    @PostMapping("/dify-context/simulate")
    public DifyContextSimulateResponse simulateDifyContext(
            @Valid @RequestBody DifyContextSimulateRequest request) {
        return knowledgeContextService.simulateDifyInputs(request);
    }

    @PostMapping("/knowledge/context/route-preview")
    public KnowledgeRoutePreviewResponse routePreview(
            @Valid @RequestBody KnowledgeRoutePreviewRequest request) {
        GroupConfigSnapshot config = groupConfigService.getConfig(request.groupId().strip());
        List<RouteKnowledgePreview> routes = Arrays.stream(KnowledgeRouteType.values())
                .map(routeType -> routePreviewItem(config, request, routeType))
                .toList();
        return new KnowledgeRoutePreviewResponse(request.groupId().strip(), request.messageText(), routes);
    }

    private RouteKnowledgePreview routePreviewItem(
            GroupConfigSnapshot config,
            KnowledgeRoutePreviewRequest request,
            KnowledgeRouteType routeType) {
        if (!routeKnowledgeEnabled(config, routeType)) {
            return new RouteKnowledgePreview(
                    routeType.name(),
                    false,
                    false,
                    0,
                    0.0d,
                    "",
                    List.of(),
                    "ROUTE_KNOWLEDGE_DISABLED");
        }
        KnowledgeContextService.KnowledgeContextBuildResult context = knowledgeContextService.buildContext(
                request.groupId(),
                request.messageText(),
                request.senderUid(),
                routeType,
                request.topK());
        List<KnowledgeContextItem> items = context.items() == null ? List.of() : context.items();
        double maxScore = items.stream()
                .mapToDouble(item -> item == null ? 0.0d : item.score())
                .max()
                .orElse(0.0d);
        return new RouteKnowledgePreview(
                routeType.name(),
                true,
                context.knowledgeUsed(),
                items.size(),
                maxScore,
                context.knowledgeContext(),
                items,
                context.knowledgeUsed() ? null : "NO_KNOWLEDGE_USED");
    }

    private boolean routeKnowledgeEnabled(GroupConfigSnapshot config, KnowledgeRouteType routeType) {
        if (config == null || !config.enableKnowledgeContext()) {
            return false;
        }
        return switch (routeType) {
            case MEME -> config.enableMemeKnowledge();
            case PASSIVE_CHAT -> config.enablePassiveChatKnowledge();
            case ACTIVE_CHAT -> config.enableActiveChatKnowledge();
        };
    }
}
