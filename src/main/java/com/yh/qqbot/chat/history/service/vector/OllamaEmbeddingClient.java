package com.yh.qqbot.chat.history.service.vector;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaEmbeddingClient {

    private final QqBotProperties properties;
    private final RestClient.Builder restClientBuilder;

    public OllamaEmbeddingClient(QqBotProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public List<Double> embed(String text) {
        QqBotProperties.Embedding embedding = properties.getKnowledge().getEmbedding();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(1, embedding.getTimeoutSeconds()));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClientBuilder
                .baseUrl(cleanBaseUrl(embedding.getBaseUrl()))
                .requestFactory(requestFactory)
                .build()
                .post()
                .uri(cleanEndpointPath(embedding.getEndpointPath()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embedding.getModel(), "input", text == null ? "" : text))
                .retrieve()
                .body(Map.class);
        return parseEmbeddingResponse(response);
    }

    public List<Double> parseEmbeddingResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("empty embedding response");
        }
        Object embeddings = response.get("embeddings");
        if (embeddings instanceof List<?> outer && !outer.isEmpty()) {
            Object first = outer.get(0);
            if (first instanceof List<?> vector) {
                return toDoubleList(vector);
            }
        }
        Object embedding = response.get("embedding");
        if (embedding instanceof List<?> vector) {
            return toDoubleList(vector);
        }
        throw new IllegalStateException("embedding response has no vector");
    }

    private List<Double> toDoubleList(List<?> values) {
        List<Double> vector = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new IllegalStateException("embedding vector contains non-number value");
            }
            vector.add(number.doubleValue());
        }
        if (vector.isEmpty()) {
            throw new IllegalStateException("embedding vector is empty");
        }
        return vector;
    }

    private String cleanBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String cleanEndpointPath(String endpointPath) {
        if (endpointPath == null || endpointPath.isBlank()) {
            return "/api/embed";
        }
        return endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
    }
}
