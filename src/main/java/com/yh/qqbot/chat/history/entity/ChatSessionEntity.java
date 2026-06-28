package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_session")
public class ChatSessionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String groupId;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer messageCount;
    private Integer memberCount;
    private String summary;
    private LocalDateTime createdAt;
}
