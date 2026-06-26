package com.yh.qqbot.adapter.dev;

import com.yh.qqbot.adapter.onebot.OneBotInboundAdapter;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.DevGroupMessageRequest;
import com.yh.qqbot.dto.RouteResult;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/dev/simulate")
public class DevMessageController {

    private final OneBotInboundAdapter oneBotInboundAdapter;

    public DevMessageController(OneBotInboundAdapter oneBotInboundAdapter) {
        this.oneBotInboundAdapter = oneBotInboundAdapter;
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
}
