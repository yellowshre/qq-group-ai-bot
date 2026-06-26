package com.yh.qqbot.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BotGroupMessageReflectionTest {

    @Test
    void effectiveTextStripsPlainTextAndPassiveTriggerUsesAtOrNickname() throws Exception {
        Object message = Class.forName("com.yh.qqbot.dto.BotGroupMessage")
                .getConstructor(String.class, String.class, String.class, String.class, String.class,
                        boolean.class, boolean.class, Instant.class)
                .newInstance("10001", "20001", "msg-1", "  哈哈  ", "  哈哈  ",
                        false, true, Instant.parse("2026-06-25T00:00:00Z"));

        assertThat(message.getClass().getMethod("effectiveText").invoke(message)).isEqualTo("哈哈");
        assertThat((Boolean) message.getClass().getMethod("triggersPassiveChat").invoke(message)).isTrue();
    }
}
