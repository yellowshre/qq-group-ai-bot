package com.yh.qqbot.chat.history.service.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OllamaEmbeddingClientTest {

    @Test
    void parsesApiEmbedResponse() throws Exception {
        Object client = client();

        @SuppressWarnings("unchecked")
        List<Double> vector = (List<Double>) invoke(client, "parseEmbeddingResponse",
                new Class<?>[]{Map.class},
                Map.of("model", "bge-m3", "embeddings", List.of(List.of(0.1d, 0.2d, 0.3d))));

        assertThat(vector).containsExactly(0.1d, 0.2d, 0.3d);
    }

    @Test
    void parsesApiEmbeddingsCompatResponse() throws Exception {
        Object client = client();

        @SuppressWarnings("unchecked")
        List<Double> vector = (List<Double>) invoke(client, "parseEmbeddingResponse",
                new Class<?>[]{Map.class},
                Map.of("embedding", List.of(1, 2, 3)));

        assertThat(vector).containsExactly(1.0d, 2.0d, 3.0d);
    }

    @Test
    void malformedResponseFailsClearly() throws Exception {
        Object client = client();

        assertThatThrownBy(() -> invoke(client, "parseEmbeddingResponse",
                new Class<?>[]{Map.class},
                Map.of("model", "bge-m3")))
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(ex -> assertThat(ex.getCause()).hasMessageContaining("no vector"));
    }

    private Object client() throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties")
                .getConstructor()
                .newInstance();
        return cls("com.yh.qqbot.chat.history.service.vector.OllamaEmbeddingClient")
                .getConstructor(properties.getClass(), RestClient.Builder.class)
                .newInstance(properties, RestClient.builder());
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
