package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_session_message")
public class ChatSessionMessageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long cleanMessageId;
    private Integer messageOrder;
    private LocalDateTime createdAt;
}
