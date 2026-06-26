package com.yh.qqbot.service.chat;

import com.yh.qqbot.dto.ChatPrompt;
import com.yh.qqbot.dto.ChatReply;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.service.context.ChatContextService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ChatReplyService {

    private final ChatContextService chatContextService;
    private final DifyWorkflowService difyWorkflowService;

    public ChatReplyService(ChatContextService chatContextService, DifyWorkflowService difyWorkflowService) {
        this.chatContextService = chatContextService;
        this.difyWorkflowService = difyWorkflowService;
    }

    public Optional<ChatReply> generate(BotGroupMessage message, GroupConfigSnapshot config, String triggerType) {
        ChatPrompt prompt = new ChatPrompt(
                message.groupId(),
                triggerType,
                config.persona(),
                chatContextService.loadHotContext(message.groupId()),
                chatContextService.loadColdSummaries(message.groupId()),
                message.effectiveText()
        );
        return difyWorkflowService.generateReply(prompt, message.userId());
    }
}
