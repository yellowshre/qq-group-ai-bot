package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.DifyContextSimulateRequest;
import com.yh.qqbot.chat.history.dto.DifyContextSimulateResponse;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewRequest;
import com.yh.qqbot.chat.history.dto.KnowledgeContextPreviewResponse;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.chat.history.service.context.KnowledgeContextService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatKnowledgeContextController {

    private final KnowledgeContextService knowledgeContextService;

    public ChatKnowledgeContextController(KnowledgeContextService knowledgeContextService) {
        this.knowledgeContextService = knowledgeContextService;
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

    @ExceptionHandler(InvalidChatCandidateRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCandidateRequest(InvalidChatCandidateRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
}
