package com.yh.qqbot.adapter.dev;

import com.yh.qqbot.adapter.onebot.QqMessageSender;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.DevFullHealthResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/dev/health")
public class HealthCheckController {

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate baseStringRedisTemplate;
    private final QqBotProperties properties;
    private final QqMessageSender messageSender;

    public HealthCheckController(
            Environment environment,
            JdbcTemplate jdbcTemplate,
            @Qualifier("baseStringRedisTemplate") StringRedisTemplate baseStringRedisTemplate,
            QqBotProperties properties,
            QqMessageSender messageSender) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.baseStringRedisTemplate = baseStringRedisTemplate;
        this.properties = properties;
        this.messageSender = messageSender;
    }

    @GetMapping("/full")
    public DevFullHealthResponse full() {
        CountResult sceneCount = count("select count(*) from scene_dict");
        CountResult enabledMemeCount = count("select count(*) from meme_material where enabled = 1");
        return new DevFullHealthResponse(
                activeProfiles(),
                mysqlStatus(sceneCount, enabledMemeCount),
                redisStatus(),
                properties.getDify().isEnabled(),
                properties.getMeme().isCachePreheatEnabled(),
                messageSender.getClass().getSimpleName(),
                sceneCount.value(),
                enabledMemeCount.value()
        );
    }

    private List<String> activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? List.of("default") : Arrays.asList(activeProfiles);
    }

    private DevFullHealthResponse.DependencyStatus mysqlStatus(CountResult sceneCount, CountResult enabledMemeCount) {
        if (!sceneCount.success()) {
            return new DevFullHealthResponse.DependencyStatus(false, sceneCount.detail());
        }
        if (!enabledMemeCount.success()) {
            return new DevFullHealthResponse.DependencyStatus(false, enabledMemeCount.detail());
        }
        return new DevFullHealthResponse.DependencyStatus(true, "ok");
    }

    private DevFullHealthResponse.DependencyStatus redisStatus() {
        try {
            String pong = baseStringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return new DevFullHealthResponse.DependencyStatus(true, pong == null ? "ok" : pong);
        } catch (Exception ex) {
            return new DevFullHealthResponse.DependencyStatus(false, rootMessage(ex));
        }
    }

    private CountResult count(String sql) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return new CountResult(true, count, "ok");
        } catch (Exception ex) {
            return new CountResult(false, null, rootMessage(ex));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage();
    }

    private record CountResult(boolean success, Long value, String detail) {
    }
}
