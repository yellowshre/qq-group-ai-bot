package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.dto.OutboundMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OneBotWsActionFactory {

    private final OneBotImagePathResolver imagePathResolver;

    public OneBotWsActionFactory(OneBotImagePathResolver imagePathResolver) {
        this.imagePathResolver = imagePathResolver;
    }

    public Map<String, Object> sendGroupMessage(String groupId, OutboundMessage outboundMessage, String echo) {
        return Map.of(
                "action", "send_group_msg",
                "params", Map.of(
                        "group_id", groupId,
                        "message", messageSegments(outboundMessage)
                ),
                "echo", echo
        );
    }

    public List<Map<String, Object>> messageSegments(OutboundMessage outboundMessage) {
        List<Map<String, Object>> segments = new ArrayList<>();
        if (outboundMessage == null) {
            return segments;
        }
        if (hasText(outboundMessage.text())) {
            segments.add(Map.of(
                    "type", "text",
                    "data", Map.of("text", outboundMessage.text())
            ));
        }
        if (hasText(outboundMessage.imagePath())) {
            segments.add(Map.of(
                    "type", "image",
                    "data", Map.of("file", normalizeImageFile(outboundMessage.imagePath()))
            ));
        }
        return segments;
    }

    public String normalizeImageFile(String imagePath) {
        return imagePathResolver.toOneBotFile(imagePath);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
