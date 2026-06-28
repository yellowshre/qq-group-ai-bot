package com.yh.qqbot.chat.history.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_member_candidate")
public class ChatMemberCandidateEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String groupId;
    private String senderUid;
    private String senderUin;
    private String senderName;
    private Long messageCount;
    private Long rawMessageCount;
    private Integer activeDays;
    private Long mentionCount;
    private Long replyCount;
    private Long repliedByCount;
    private Long sessionCount;
    private Long score;
    private String candidateReason;
    private String status;
    private String reviewer;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
