package com.yh.qqbot.service.log;

import com.yh.qqbot.entity.AdminOpLogEntity;
import com.yh.qqbot.mapper.AdminOpLogMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AdminOpLogService {

    private static final Logger log = LoggerFactory.getLogger(AdminOpLogService.class);

    private final AdminOpLogMapper adminOpLogMapper;

    public AdminOpLogService(AdminOpLogMapper adminOpLogMapper) {
        this.adminOpLogMapper = adminOpLogMapper;
    }

    @Async("botTaskExecutor")
    public void record(String groupId, String operatorUid, String operation, String detail) {
        try {
            AdminOpLogEntity entity = new AdminOpLogEntity();
            entity.setGroupId(Long.valueOf(groupId));
            entity.setOperatorUid(Long.valueOf(operatorUid));
            entity.setOperation(operation);
            entity.setDetail(detail);
            entity.setCreatedAt(LocalDateTime.now());
            adminOpLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to record admin op log. groupId={}, operatorUid={}, operation={}",
                    groupId, operatorUid, operation, ex);
        }
    }
}
