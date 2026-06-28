package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_knowledge_candidate")
public class ChatKnowledgeCandidateEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String groupId;
    private String candidateType;
    private String title;
    private String content;
    private Long sourceSessionId;
    private String sourceMessageIds;
    private String evidenceText;
    private Long hitCount;
    private Long memberCount;
    private BigDecimal confidence;
    private String status;
    private String reviewer;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
