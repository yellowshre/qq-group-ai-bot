package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.OutboundMessage;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!dev")
@ConditionalOnProperty(prefix = "qqbot.onebot.ws", name = "enabled", havingValue = "false", matchIfMissing = true)
public class OneBotHttpMessageSender implements QqMessageSender {

    private static final Logger log = LoggerFactory.getLogger(OneBotHttpMessageSender.class);

    private final QqBotProperties properties;
    private final RestClient restClient;
    private final OneBotImagePathResolver imagePathResolver;

    public OneBotHttpMessageSender(
            QqBotProperties properties,
            RestClient.Builder builder,
            OneBotImagePathResolver imagePathResolver) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getOnebot().getApiServerHost()).build();
        this.imagePathResolver = imagePathResolver;
    }

    @PostConstruct
    public void logSenderType() {
        log.info("QQ message sender active: OneBotHttpMessageSender, dryRun={}", properties.getOnebot().isDryRun());
    }

    @Override
    public boolean sendGroupMessage(String groupId, OutboundMessage outboundMessage) {
        if (properties.getOnebot().isDryRun()) {
            log.info("OneBot dry-run send. groupId={}, text={}, imagePath={}",
                    groupId, outboundMessage.text(), outboundMessage.imagePath());
            return true;
        }

        try {
            Map<String, Object> body = Map.of(
                    "group_id", groupId,
                    "message", messageSegments(outboundMessage),
                    "auto_escape", false
            );
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri("/send_group_msg")
                    .contentType(MediaType.APPLICATION_JSON);
            String token = properties.getOnebot().getAccessToken();
            if (token != null && !token.isBlank()) {
                spec.header("Authorization", "Bearer " + token);
            }
            spec.body(body).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("OneBot send_group_msg failed. groupId={}", groupId, ex);
            return false;
        }
    }

    @Override
    public boolean sendPrivateMessage(String userId, OutboundMessage outboundMessage) {
        if (properties.getOnebot().isDryRun()) {
            log.info("OneBot dry-run private send. userId={}, text={}, imagePath={}",
                    userId, outboundMessage.text(), outboundMessage.imagePath());
            return true;
        }

        try {
            Map<String, Object> body = Map.of(
                    "user_id", userId,
                    "message", messageSegments(outboundMessage),
                    "auto_escape", false
            );
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri("/send_private_msg")
                    .contentType(MediaType.APPLICATION_JSON);
            String token = properties.getOnebot().getAccessToken();
            if (token != null && !token.isBlank()) {
                spec.header("Authorization", "Bearer " + token);
            }
            spec.body(body).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("OneBot send_private_msg failed. userId={}", userId, ex);
            return false;
        }
    }

    private List<Map<String, Object>> messageSegments(OutboundMessage outboundMessage) {
        List<Map<String, Object>> segments = new ArrayList<>();
        if (outboundMessage.text() != null && !outboundMessage.text().isBlank()) {
            segments.add(Map.of(
                    "type", "text",
                    "data", Map.of("text", outboundMessage.text())
            ));
        }
        if (outboundMessage.imagePath() != null && !outboundMessage.imagePath().isBlank()) {
            segments.add(Map.of(
                    "type", "image",
                    "data", Map.of("file", imagePathResolver.toOneBotFile(outboundMessage.imagePath()))
            ));
        }
        return segments;
    }
}
