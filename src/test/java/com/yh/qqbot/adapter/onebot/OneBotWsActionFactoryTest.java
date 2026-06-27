package com.yh.qqbot.adapter.onebot;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OneBotWsActionFactoryTest {

    @Test
    void buildsTextSendGroupMessageAction() throws Exception {
        Object factory = factory();
        Map<String, Object> action = sendGroupMessage(
                factory,
                "736566774",
                outboundText("Spring Boot WebSocket 发送链路预演"),
                "test-send-001");

        assertThat(action.get("action")).isEqualTo("send_group_msg");
        assertThat(action.get("echo")).isEqualTo("test-send-001");

        Map<?, ?> params = (Map<?, ?>) action.get("params");
        assertThat(params.get("group_id")).isEqualTo("736566774");
        List<?> message = (List<?>) params.get("message");
        assertThat(message).hasSize(1);
        Map<?, ?> segment = (Map<?, ?>) message.get(0);
        assertThat(segment.get("type")).isEqualTo("text");
        assertThat(((Map<?, ?>) segment.get("data")).get("text")).isEqualTo("Spring Boot WebSocket 发送链路预演");
    }

    @Test
    void buildsImageSendGroupMessageActionAndNormalizesWindowsPath() throws Exception {
        Object factory = factory();
        Map<String, Object> action = sendGroupMessage(
                factory,
                "736566774",
                outboundImage("C:/qqbot/memes/laugh_01.png"),
                "test-send-002");

        Map<?, ?> params = (Map<?, ?>) action.get("params");
        List<?> message = (List<?>) params.get("message");
        assertThat(message).hasSize(1);
        Map<?, ?> segment = (Map<?, ?>) message.get(0);
        assertThat(segment.get("type")).isEqualTo("image");
        assertThat(((Map<?, ?>) segment.get("data")).get("file"))
                .isEqualTo("file:///C:/qqbot/memes/laugh_01.png");
    }

    @Test
    void keepsExistingUrlOrFileImageReference() throws Exception {
        Object factory = factory();

        assertThat(normalizeImageFile(factory, "file:///C:/qqbot/memes/laugh_01.png"))
                .isEqualTo("file:///C:/qqbot/memes/laugh_01.png");
        assertThat(normalizeImageFile(factory, "https://example.com/a.png"))
                .isEqualTo("https://example.com/a.png");
        assertThat(normalizeImageFile(factory, "base64://abc"))
                .isEqualTo("base64://abc");
    }

    private Object factory() throws Exception {
        return cls("com.yh.qqbot.adapter.onebot.OneBotWsActionFactory").getConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendGroupMessage(Object factory, String groupId, Object outboundMessage, String echo)
            throws Exception {
        return (Map<String, Object>) factory.getClass()
                .getMethod("sendGroupMessage", String.class, cls("com.yh.qqbot.dto.OutboundMessage"), String.class)
                .invoke(factory, groupId, outboundMessage, echo);
    }

    private String normalizeImageFile(Object factory, String imagePath) throws Exception {
        return (String) factory.getClass().getMethod("normalizeImageFile", String.class).invoke(factory, imagePath);
    }

    private Object outboundText(String text) throws Exception {
        Method method = cls("com.yh.qqbot.dto.OutboundMessage").getMethod("text", String.class);
        return method.invoke(null, text);
    }

    private Object outboundImage(String imagePath) throws Exception {
        Method method = cls("com.yh.qqbot.dto.OutboundMessage").getMethod("image", String.class);
        return method.invoke(null, imagePath);
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
