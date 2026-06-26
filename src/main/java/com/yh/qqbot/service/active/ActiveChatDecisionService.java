package com.yh.qqbot.service.active;

import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.service.chat.DifyWorkflowService;
import com.yh.qqbot.service.context.ChatContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActiveChatDecisionService {

    private static final Logger log = LoggerFactory.getLogger(ActiveChatDecisionService.class);

    private final ChatContextService chatContextService;
    private final DifyWorkflowService difyWorkflowService;

    public ActiveChatDecisionService(ChatContextService chatContextService, DifyWorkflowService difyWorkflowService) {
        this.chatContextService = chatContextService;
        this.difyWorkflowService = difyWorkflowService;
    }

    public boolean shouldJoin(BotGroupMessage message, GroupConfigSnapshot config) {
        try {
            return difyWorkflowService.shouldActiveJoin(
                    message.groupId(),
                    config.persona(),
                    message.effectiveText(),
                    chatContextService.recentMessagesForActiveDecision(message.groupId())
            );
        } catch (Exception ex) {
            log.warn("Active chat decision failed. groupId={}, messageId={}",
                    message.groupId(), message.messageId(), ex);
            return false;
        }
    }
}
