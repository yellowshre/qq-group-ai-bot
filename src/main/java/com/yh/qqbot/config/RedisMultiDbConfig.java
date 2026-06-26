package com.yh.qqbot.config;

import com.yh.qqbot.config.properties.QqBotProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisMultiDbConfig {

    @Bean
    @Primary
    public LettuceConnectionFactory baseRedisConnectionFactory(QqBotProperties properties) {
        return connectionFactory(properties, properties.getRedis().getDatabase().getBase());
    }

    @Bean
    public LettuceConnectionFactory chatRedisConnectionFactory(QqBotProperties properties) {
        return connectionFactory(properties, properties.getRedis().getDatabase().getChat());
    }

    @Bean
    public LettuceConnectionFactory memeRedisConnectionFactory(QqBotProperties properties) {
        return connectionFactory(properties, properties.getRedis().getDatabase().getMeme());
    }

    @Bean
    public LettuceConnectionFactory rateRedisConnectionFactory(QqBotProperties properties) {
        return connectionFactory(properties, properties.getRedis().getDatabase().getRate());
    }

    @Bean
    @Primary
    public StringRedisTemplate baseStringRedisTemplate(
            @Qualifier("baseRedisConnectionFactory") LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StringRedisTemplate chatStringRedisTemplate(
            @Qualifier("chatRedisConnectionFactory") LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StringRedisTemplate memeStringRedisTemplate(
            @Qualifier("memeRedisConnectionFactory") LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StringRedisTemplate rateStringRedisTemplate(
            @Qualifier("rateRedisConnectionFactory") LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private LettuceConnectionFactory connectionFactory(QqBotProperties properties, int database) {
        QqBotProperties.Redis redis = properties.getRedis();
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        redisConfig.setDatabase(database);
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            redisConfig.setPassword(RedisPassword.of(redis.getPassword()));
        }
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(redis.getTimeout())
                .build();
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
