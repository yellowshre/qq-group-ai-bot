package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_clean_message")
public class ChatCleanMessageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long rawMessageId;
    private Long batchId;
    private String groupId;
    private String messageId;
    private Long seq;
    private LocalDateTime messageTime;
    private String senderUid;
    private String senderUin;
    private String senderName;
    private String cleanText;
    private Integer textLength;
    private Boolean isReply;
    private Boolean hasMention;
    private LocalDateTime createdAt;
}
