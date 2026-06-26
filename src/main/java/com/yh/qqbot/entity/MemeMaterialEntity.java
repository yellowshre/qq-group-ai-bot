package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("meme_material")
public class MemeMaterialEntity {

    @TableId(value = "meme_id", type = IdType.AUTO)
    private Long memeId;
    private String keywords;
    private String sceneCode;
    private String sceneDesc;
    private Integer weight;
    private Boolean enabled;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
