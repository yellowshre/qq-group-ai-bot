package com.yh.qqbot.adapter.dify;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DifyClient {

    private static final Logger log = LoggerFactory.getLogger(DifyClient.class);

    private final QqBotProperties properties;
    private final RestClient restClient;

    public DifyClient(QqBotProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(Math.max(1, properties.getDify().getTimeoutMs()));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = builder
                .baseUrl(properties.getDify().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<Map<String, Object>> runWorkflow(String workflowId, Map<String, Object> inputs, String userId) {
        return runWorkflow(workflowId, inputs, userId, properties.getDify().getApiKey());
    }

    public Optional<Map<String, Object>> runWorkflow(
            String workflowId,
            Map<String, Object> inputs,
            String userId,
            String apiKey) {
        QqBotProperties.Dify dify = properties.getDify();
        if (!dify.isEnabled()) {
            log.debug("Skip Dify workflow because Dify is disabled.");
            return Optional.empty();
        }
        if (workflowId == null || workflowId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.debug("Skip Dify workflow because workflow id or api key is blank.");
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "workflow_id", workflowId,
                    "inputs", inputs,
                    "response_mode", "blocking",
                    "user", userId == null || userId.isBlank() ? "qqbot" : userId
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/workflows/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Dify workflow call failed. workflowId={}", workflowId, ex);
            return Optional.empty();
        }
    }
}
