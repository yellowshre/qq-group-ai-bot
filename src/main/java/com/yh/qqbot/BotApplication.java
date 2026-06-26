package com.yh.qqbot;

import com.yh.qqbot.config.properties.QqBotProperties;
import love.forte.simbot.spring.EnableSimbot;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableSimbot
@EnableAsync
@MapperScan("com.yh.qqbot.mapper")
@SpringBootApplication
@EnableConfigurationProperties(QqBotProperties.class)
public class BotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
