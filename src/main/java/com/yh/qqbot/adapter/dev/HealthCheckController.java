package com.yh.qqbot.adapter.dev;

import com.yh.qqbot.adapter.onebot.QqMessageSender;
import com.yh.qqbot.config.properties.QqBotProperties;
import com.yh.qqbot.dto.DevFullHealthResponse;
import com.yh.qqbot.dto.GroupConfigSnapshot;
import com.yh.qqbot.service.config.GroupConfigService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"dev", "local"})
@RequestMapping("/dev/health")
public class HealthCheckController {

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate baseStringRedisTemplate;
    private final QqBotProperties properties;
    private final QqMessageSender messageSender;
    private final GroupConfigService groupConfigService;

    public HealthCheckController(
            Environment environment,
            JdbcTemplate jdbcTemplate,
            @Qualifier("baseStringRedisTemplate") StringRedisTemplate baseStringRedisTemplate,
            QqBotProperties properties,
            QqMessageSender messageSender,
            GroupConfigService groupConfigService) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.baseStringRedisTemplate = baseStringRedisTemplate;
        this.properties = properties;
        this.messageSender = messageSender;
        this.groupConfigService = groupConfigService;
    }

    @GetMapping("/full")
    public DevFullHealthResponse full(@RequestParam(required = false) String groupId) {
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
                enabledMemeCount.value(),
                oneBotStatus(),
                difyStatus(),
                properties.getMeme().getBaseDir(),
                groupConfig(groupId)
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

    private DevFullHealthResponse.OneBotStatus oneBotStatus() {
        return new DevFullHealthResponse.OneBotStatus(
                properties.getOnebot().getWs().isEnabled(),
                properties.getOnebot().getSelfId(),
                properties.getOnebot().getAllowedGroupIds());
    }

    private DevFullHealthResponse.DifyStatus difyStatus() {
        QqBotProperties.Dify dify = properties.getDify();
        return new DevFullHealthResponse.DifyStatus(
                dify.isEnabled(),
                hasText(dify.getBaseUrl()),
                hasText(dify.getSceneWorkflowId()),
                hasText(dify.getPassiveChatWorkflowId()),
                hasText(dify.getActiveWorkflowId()),
                hasText(dify.getMemeSceneApiKey()),
                hasText(dify.getPassiveChatApiKey()),
                hasText(dify.getActiveChatApiKey()));
    }

    private GroupConfigSnapshot groupConfig(String groupId) {
        if (!hasText(groupId)) {
            return null;
        }
        try {
            return groupConfigService.getConfig(groupId.strip());
        } catch (Exception ex) {
            return null;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CountResult(boolean success, Long value, String detail) {
    }
}
