package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_import_batch")
public class ChatImportBatchEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private String sourceFile;
    private String sourceHash;
    private String chatName;
    private String exporterName;
    private String exporterVersion;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalMessages;
    private Long rawCount;
    private Long cleanCount;
    private Long mentionCount;
    private Long replyCount;
    private Long sessionCount;
    private Long memberCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
