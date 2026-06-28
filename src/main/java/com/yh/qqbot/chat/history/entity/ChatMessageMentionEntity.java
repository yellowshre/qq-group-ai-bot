package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_message_mention")
public class ChatMessageMentionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long rawMessageId;
    private String groupId;
    private String messageId;
    private String senderUid;
    private String mentionedUid;
    private String mentionedName;
    private String mentionType;
    private LocalDateTime createdAt;
}
