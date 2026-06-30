package com.yh.qqbot.adapter.dev;

import com.yh.qqbot.adapter.onebot.OneBotInboundAdapter;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.BotPrivateMessage;
import com.yh.qqbot.dto.CommandHandleResult;
import com.yh.qqbot.dto.DevGroupMessageRequest;
import com.yh.qqbot.dto.DevPrivateMessageRequest;
import com.yh.qqbot.dto.DevPrivateMessageResult;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.service.command.PrivateAdminCommandService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/simulate")
public class DevMessageController {

    private final OneBotInboundAdapter oneBotInboundAdapter;
    private final PrivateAdminCommandService privateAdminCommandService;

    public DevMessageController(
            OneBotInboundAdapter oneBotInboundAdapter,
            PrivateAdminCommandService privateAdminCommandService) {
        this.oneBotInboundAdapter = oneBotInboundAdapter;
        this.privateAdminCommandService = privateAdminCommandService;
    }

    @PostMapping("/group-message")
    public RouteResult groupMessage(@Valid @RequestBody DevGroupMessageRequest request) {
        String messageId = request.messageId() == null || request.messageId().isBlank()
                ? "dev-" + UUID.randomUUID()
                : request.messageId();
        BotGroupMessage message = new BotGroupMessage(
                request.groupId(),
                request.userId(),
                messageId,
                request.rawMessage(),
                request.rawMessage(),
                Boolean.TRUE.equals(request.atBot()),
                Boolean.TRUE.equals(request.botNicknameMatched()),
                Instant.now()
        );
        return oneBotInboundAdapter.handleGroupMessage(message);
    }

    @PostMapping("/private-message")
    public DevPrivateMessageResult privateMessage(@Valid @RequestBody DevPrivateMessageRequest request) {
        String messageId = request.messageId() == null || request.messageId().isBlank()
                ? "dev-private-" + UUID.randomUUID()
                : request.messageId();
        BotPrivateMessage message = new BotPrivateMessage(
                request.userId(),
                messageId,
                request.rawMessage(),
                request.rawMessage(),
                Instant.now()
        );
        CommandHandleResult result = privateAdminCommandService.tryHandle(message);
        return DevPrivateMessageResult.from(request.userId(), messageId, result);
    }
}
