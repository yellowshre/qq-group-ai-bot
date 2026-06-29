package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("group_config")
public class GroupConfigEntity {

    @TableId("group_id")
    private Long groupId;
    private Boolean botOn;
    private Boolean enableChat;
    private Boolean enableMeme;
    private Boolean enablePassiveChat;
    private Boolean enableAutoJoin;
    private Long activeCooldownSeconds;
    private Long activeHourLimit;
    private Long activeDayLimit;
    private String safeWord;
    private String safeWordReply;
    private String persona;
    private String memoryMode;
    private Boolean enableKnowledgeContext;
    private Boolean enableMemeKnowledge;
    private Boolean enablePassiveChatKnowledge;
    private Boolean enableActiveChatKnowledge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
