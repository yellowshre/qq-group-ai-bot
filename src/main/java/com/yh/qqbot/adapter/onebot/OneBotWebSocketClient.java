package com.yh.qqbot.adapter.onebot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yh.qqbot.config.properties.QqBotProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "qqbot.onebot.ws", name = "enabled", havingValue = "true")
public class OneBotWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OneBotWebSocketClient.class);

    private final QqBotProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<OneBotWsEventHandler> eventHandlerProvider;
    private final Executor botTaskExecutor;
    private final HttpClient httpClient;
    private final ScheduledExecutorService reconnectExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private volatile WebSocket webSocket;

    public OneBotWebSocketClient(
            QqBotProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<OneBotWsEventHandler> eventHandlerProvider,
            @Qualifier("botTaskExecutor") Executor botTaskExecutor) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventHandlerProvider = eventHandlerProvider;
        this.botTaskExecutor = botTaskExecutor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getOnebot().getWs().getReconnectDelayMs())))
                .build();
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "onebot-ws-reconnect");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PostConstruct
    public void start() {
        logWsConfig();
        running.set(true);
        connect();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        connected.set(false);
        WebSocket current = webSocket;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        reconnectExecutor.shutdownNow();
    }

    public boolean sendAction(Map<String, Object> action) {
        try {
            return sendText(objectMapper.writeValueAsString(action));
        } catch (Exception ex) {
            log.warn("Failed to serialize OneBot WebSocket action.", ex);
            return false;
        }
    }

    public boolean sendText(String text) {
        WebSocket current = webSocket;
        if (!connected.get() || current == null) {
            log.warn("OneBot WebSocket is not connected. Skip send.");
            return false;
        }
        try {
            current.sendText(text, true).whenComplete((ignored, ex) -> {
                if (ex != null) {
                    log.warn("OneBot WebSocket send failed.", ex);
                }
            });
            return true;
        } catch (Exception ex) {
            log.warn("OneBot WebSocket send failed.", ex);
            return false;
        }
    }

    boolean isConnected() {
        return connected.get();
    }

    URI connectionUri() {
        QqBotProperties.Ws ws = properties.getOnebot().getWs();
        String url = ws.getUrl() == null || ws.getUrl().isBlank()
                ? "ws://127.0.0.1:3001/"
                : ws.getUrl().strip();
        String token = ws.getAccessToken();
        if (token == null || token.isBlank()) {
            return URI.create(url);
        }
        String separator = url.contains("?") ? "&" : "?";
        return URI.create(url + separator + "access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }

    private void logWsConfig() {
        QqBotProperties.OneBot onebot = properties.getOnebot();
        QqBotProperties.Ws ws = onebot.getWs();
        log.info("OneBot WebSocket config. enabled={}, selfId={}, allowedGroupIds={}, tokenConfigured={}, wsUrl={}",
                ws.isEnabled(), onebot.getSelfId(), onebot.getAllowedGroupIds(),
                hasText(ws.getAccessToken()), ws.getUrl());
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        URI uri = connectionUri();
        QqBotProperties.Ws ws = properties.getOnebot().getWs();
        log.info("Connecting OneBot WebSocket. wsUrl={}, tokenConfigured={}, connectionUri={}",
                ws.getUrl(), hasText(ws.getAccessToken()), maskAccessToken(uri));
        httpClient.newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .whenComplete((socket, ex) -> {
                    if (ex != null) {
                        connected.set(false);
                        log.warn("OneBot WebSocket connect failed. retryDelayMs={}",
                                properties.getOnebot().getWs().getReconnectDelayMs(), ex);
                        scheduleReconnect();
                    } else {
                        webSocket = socket;
                    }
                });
    }

    private void scheduleReconnect() {
        if (!running.get() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        long delayMs = Math.max(1, properties.getOnebot().getWs().getReconnectDelayMs());
        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private String maskAccessToken(URI uri) {
        String value = uri.toString();
        return value.replaceAll("([?&]access_token=)[^&]*", "$1***");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private final class Listener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            connected.set(true);
            log.info("OneBot WebSocket connected.");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            synchronized (buffer) {
                buffer.append(data);
                if (last) {
                    String payload = buffer.toString();
                    buffer.setLength(0);
                    log.debug("OneBot raw websocket payload: {}", payload);
                    botTaskExecutor.execute(() -> {
                        try {
                            OneBotWsEventHandler handler = eventHandlerProvider.getIfAvailable();
                            if (handler != null) {
                                handler.handle(payload);
                            } else {
                                log.warn("OneBot WebSocket event handler is not available. Skip payload.");
                            }
                        } catch (Exception ex) {
                            log.warn("OneBot WebSocket payload handling failed.", ex);
                        }
                    });
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected.set(false);
            log.warn("OneBot WebSocket closed. statusCode={}, reason={}", statusCode, reason);
            scheduleReconnect();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected.set(false);
            log.warn("OneBot WebSocket error.", error);
            scheduleReconnect();
        }
    }
}
