package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.ChatHistoryImportRequest;
import com.yh.qqbot.chat.history.dto.ChatHistoryImportResponse;
import com.yh.qqbot.chat.history.service.ChatHistoryImportService;
import com.yh.qqbot.chat.history.service.InvalidChatHistoryFilePathException;
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
public class ChatHistoryImportController {

    private final ChatHistoryImportService importService;

    public ChatHistoryImportController(ChatHistoryImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ChatHistoryImportResponse importHistory(@Valid @RequestBody ChatHistoryImportRequest request) {
        return importService.importFile(request.groupId(), request.filePath());
    }

    @ExceptionHandler(InvalidChatHistoryFilePathException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFilePath(InvalidChatHistoryFilePathException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
}
