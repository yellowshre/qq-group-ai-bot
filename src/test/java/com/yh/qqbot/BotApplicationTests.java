package com.yh.qqbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "qqbot.meme.cache-preheat-enabled=false")
class BotApplicationTests {

    @Test
    void contextLoads() {
    }
}
