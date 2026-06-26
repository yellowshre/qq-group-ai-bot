package com.yh.qqbot.service.log;

import com.yh.qqbot.dto.BotGroupMessage;
import com.yh.qqbot.dto.RouteResult;
import com.yh.qqbot.entity.TriggerLogEntity;
import com.yh.qqbot.mapper.TriggerLogMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TriggerLogService {

    private static final Logger log = LoggerFactory.getLogger(TriggerLogService.class);

    private final TriggerLogMapper triggerLogMapper;

    public TriggerLogService(TriggerLogMapper triggerLogMapper) {
        this.triggerLogMapper = triggerLogMapper;
    }

    @Async("botTaskExecutor")
    public void record(BotGroupMessage message, RouteResult result, boolean success, String errorMsg) {
        try {
            TriggerLogEntity entity = new TriggerLogEntity();
            entity.setGroupId(Long.valueOf(message.groupId()));
            entity.setUserId(Long.valueOf(message.userId()));
            entity.setMessageId(message.messageId());
            entity.setOriginalMsg(message.rawMessage());
            entity.setResponseType(result.responseType());
            entity.setResponseText(result.outboundMessage() == null ? null : result.outboundMessage().text());
            entity.setMemeId(result.memeId());
            entity.setWorkflowType(result.workflowType());
            entity.setDurationMs(result.durationMs());
            entity.setSuccess(success);
            entity.setErrorMsg(errorMsg);
            entity.setCreatedAt(LocalDateTime.now());
            triggerLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to record trigger log. groupId={}, messageId={}",
                    message.groupId(), message.messageId(), ex);
        }
    }
}
