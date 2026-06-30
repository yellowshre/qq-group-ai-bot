package com.yh.qqbot.dto;

import java.math.BigDecimal;

public record AdminSceneDictRequest(
        String sceneDesc,
        BigDecimal confidenceThreshold
) {
}
