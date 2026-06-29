package com.yh.qqbot.chat.history.controller;

import com.yh.qqbot.chat.history.dto.ChatMemberRankRequest;
import com.yh.qqbot.chat.history.dto.ChatMemberRankResponse;
import com.yh.qqbot.chat.history.service.InvalidChatCandidateRequestException;
import com.yh.qqbot.chat.history.service.rank.ChatMemberRankService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/chat-history")
public class ChatMemberRankController {

    private final ChatMemberRankService rankService;

    public ChatMemberRankController(ChatMemberRankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping("/member-rank")
    public ChatMemberRankResponse rank(
            @RequestParam String groupId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "MESSAGE") String rankType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer topN) {
        return rankService.rank(new ChatMemberRankRequest(groupId, batchId, rankType, startDate, endDate, topN));
    }

    @PostMapping("/member-rank")
    public ChatMemberRankResponse rank(@Valid @RequestBody ChatMemberRankRequest request) {
        return rankService.rank(request);
    }

    @ExceptionHandler(InvalidChatCandidateRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCandidateRequest(InvalidChatCandidateRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
}
