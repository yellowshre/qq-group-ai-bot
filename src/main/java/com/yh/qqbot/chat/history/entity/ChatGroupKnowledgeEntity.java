package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_group_knowledge")
public class ChatGroupKnowledgeEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private Long sourceCandidateId;
    private String knowledgeType;
    private String title;
    private String content;
    private String evidenceText;
    private String status;
    private Boolean enabled;
    private Integer version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
