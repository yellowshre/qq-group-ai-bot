package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chat_summary")
public class ChatSummaryEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String summaryText;
    private LocalDateTime createdAt;
}
