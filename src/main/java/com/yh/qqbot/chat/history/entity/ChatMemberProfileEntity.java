package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_member_profile")
public class ChatMemberProfileEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String groupId;
    private Long sourceMemberCandidateId;
    private String senderUid;
    private String senderUin;
    private String senderName;
    private String profileText;
    private Long messageCount;
    private Long rawMessageCount;
    private Integer activeDays;
    private Long mentionCount;
    private Long replyCount;
    private Long repliedByCount;
    private Long sessionCount;
    private String status;
    private Boolean enabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
