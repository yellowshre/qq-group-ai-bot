package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("admin_op_log")
public class AdminOpLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long operatorUid;
    private String operation;
    private String detail;
    private LocalDateTime createdAt;
}
