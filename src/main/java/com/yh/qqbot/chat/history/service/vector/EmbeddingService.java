package com.yh.qqbot.chat.history.service.vector;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final QqBotProperties properties;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    public EmbeddingService(QqBotProperties properties, OllamaEmbeddingClient ollamaEmbeddingClient) {
        this.properties = properties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
    }

    public List<Double> embed(String text) {
        QqBotProperties.Embedding embedding = properties.getKnowledge().getEmbedding();
        if (!embedding.isEnabled()) {
            throw new IllegalStateException("embedding disabled");
        }
        if (!"ollama".equalsIgnoreCase(embedding.getProvider())) {
            throw new IllegalStateException("unsupported embedding provider");
        }
        return ollamaEmbeddingClient.embed(text);
    }

    public String model() {
        return properties.getKnowledge().getEmbedding().getModel();
    }

    public boolean enabled() {
        return properties.getKnowledge().getEmbedding().isEnabled();
    }
}
