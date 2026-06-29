package com.yh.qqbot.chat.history.service.vector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class VectorSimilarityUtils {

    private VectorSimilarityUtils() {
    }

    public static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < left.size(); i++) {
            double l = value(left.get(i));
            double r = value(right.get(i));
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public static double round4(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static double value(Double value) {
        return value == null ? 0.0d : value;
    }
}
