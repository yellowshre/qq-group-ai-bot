package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_message_reply")
public class ChatMessageReplyEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long rawMessageId;
    private String groupId;
    private String messageId;
    private String replyMessageId;
    private String replySenderUin;
    private String replySenderName;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createdAt;
}
