package com.yh.qqbot.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("scene_dict")
public class SceneDictEntity {

    @TableId("scene_code")
    private String sceneCode;
    private String sceneDesc;
    private BigDecimal confidenceThreshold;
}
