package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_raw_message")
public class ChatRawMessageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String groupId;
    private String messageId;
    private Long seq;
    private LocalDateTime messageTime;
    private String senderUid;
    private String senderUin;
    private String senderName;
    private String senderGroupCard;
    private String messageType;
    private String rawText;
    private Boolean systemFlag;
    private Boolean recalledFlag;
    private Boolean hasResource;
    private String elementTypes;
    private String rawJson;
    private LocalDateTime createdAt;
}
