package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("trigger_log")
public class TriggerLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    private String messageId;
    private String originalMsg;
    private String responseType;
    private String responseText;
    private Long memeId;
    private String workflowType;
    private Integer tokenUsed;
    private BigDecimal cost;
    private Long durationMs;
    private Boolean success;
    private String errorMsg;
    private LocalDateTime createdAt;
}
