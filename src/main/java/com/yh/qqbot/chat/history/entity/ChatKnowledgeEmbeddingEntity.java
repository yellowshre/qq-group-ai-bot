package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_knowledge_embedding")
public class ChatKnowledgeEmbeddingEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private String targetType;
    private Long targetId;
    private String embeddingModel;
    private Integer embeddingDim;
    private String embeddingText;
    private String embeddingVector;
    private String embeddingHash;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
