package com.yh.qqbot.chat.history.service.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VectorSimilarityUtilsTest {

    @Test
    void cosineSimilarityHandlesNormalAndInvalidVectors() throws Exception {
        Class<?> utils = cls("com.yh.qqbot.chat.history.service.vector.VectorSimilarityUtils");

        assertThat(cosine(utils, List.of(1.0d, 0.0d), List.of(1.0d, 0.0d))).isEqualTo(1.0d);
        assertThat(cosine(utils, List.of(1.0d, 0.0d), List.of(0.0d, 1.0d))).isEqualTo(0.0d);
        assertThat(cosine(utils, List.of(), List.of(1.0d))).isEqualTo(0.0d);
        assertThat(cosine(utils, List.of(1.0d), List.of(1.0d, 2.0d))).isEqualTo(0.0d);
    }

    @Test
    void round4UsesHalfUp() throws Exception {
        Class<?> utils = cls("com.yh.qqbot.chat.history.service.vector.VectorSimilarityUtils");

        Object rounded = utils.getMethod("round4", double.class).invoke(null, 0.12345d);

        assertThat(rounded).isEqualTo(0.1235d);
    }

    private double cosine(Class<?> utils, List<Double> left, List<Double> right) throws Exception {
        return (double) utils.getMethod("cosineSimilarity", List.class, List.class).invoke(null, left, right);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
