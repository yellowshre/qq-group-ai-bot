package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_member_stat")
public class ChatMemberStatEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String groupId;
    private String senderUid;
    private String senderUin;
    private String senderName;
    private Long rawMessageCount;
    private Long messageCount;
    private Integer activeDays;
    private Long mentionCount;
    private Long replyCount;
    private Long repliedByCount;
    private Long sessionCount;
    private LocalDateTime createdAt;
}
