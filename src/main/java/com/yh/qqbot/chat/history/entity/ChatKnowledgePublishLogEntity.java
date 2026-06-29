package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_knowledge_publish_log")
public class ChatKnowledgePublishLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String sourceType;
    private Long sourceId;
    private String targetType;
    private Long targetId;
    private String action;
    private String operator;
    private String comment;
    private LocalDateTime createdAt;
}
