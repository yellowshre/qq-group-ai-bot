package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_knowledge_review_log")
public class ChatKnowledgeReviewLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private String oldStatus;
    private String newStatus;
    private String reviewer;
    private String reviewComment;
    private LocalDateTime createdAt;
}
