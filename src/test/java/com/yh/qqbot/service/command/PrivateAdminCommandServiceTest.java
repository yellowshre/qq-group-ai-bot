package com.yh.qqbot.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PrivateAdminCommandServiceTest {

    @Test
    void nonAdminPrivateCommandIsIgnoredSilently() throws Exception {
        Fixture fixture = fixture(false, true, List.of("736566774"));

        Object result = invoke(fixture.service(), "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                message("10000", "#群 736566774 状态"));

        assertThat(invoke(result, "handled")).isEqualTo(false);
        assertThat(invoke(result, "outboundMessage")).isNull();
        assertThat(invoke(result, "operation")).isNull();
        Mockito.verifyNoInteractions(fixture.adminOpLogService());
    }

    @Test
    void disabledPrivateAdminReturnsDisabledForAdmin() throws Exception {
        Fixture fixture = fixture(true, false, List.of("736566774"));

        Object result = invoke(fixture.service(), "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                message("885391366", "#群 736566774 状态"));

        assertThat(invoke(result, "handled")).isEqualTo(true);
        assertThat(invoke(result, "operation")).isEqualTo("PRIVATE_ADMIN_DISABLED");
        assertThat(invoke(invoke(result, "outboundMessage"), "text")).isEqualTo("private disabled");
        Mockito.verifyNoInteractions(fixture.adminOpLogService());
    }

    @Test
    void groupOutsideAllowedListIsRejected() throws Exception {
        Fixture fixture = fixture(true, true, List.of("123"));

        Object result = invoke(fixture.service(), "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                message("885391366", "#群 736566774 状态"));

        assertThat(invoke(result, "handled")).isEqualTo(true);
        assertThat(invoke(result, "operation")).isEqualTo("GROUP_NOT_ALLOWED");
        assertThat(invoke(invoke(result, "outboundMessage"), "text")).isEqualTo("group not allowed");
        Mockito.verifyNoInteractions(fixture.adminOpLogService());
    }

    @Test
    void adminCanDisableMemeRouteByPrivateMessage() throws Exception {
        AtomicReference<Object> updated = new AtomicReference<>();
        Fixture fixture = fixture(true, true, List.of("736566774"), updated);

        Object result = invoke(fixture.service(), "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                message("885391366", "#群 736566774 关闭表情包"));

        assertThat(invoke(result, "handled")).isEqualTo(true);
        assertThat(invoke(result, "operation")).isEqualTo("MEME_OFF");
        assertThat(invoke(invoke(result, "outboundMessage"), "text")).isEqualTo("success");
        assertThat(invoke(updated.get(), "enableMeme")).isEqualTo(false);
        Object verified = Mockito.verify(fixture.adminOpLogService());
        invoke(verified, "record",
                new Class<?>[]{String.class, String.class, String.class, String.class},
                "736566774", "885391366", "MEME_OFF", "enable_meme=false");
    }

    @Test
    void statusReturnsGroupConfigSummary() throws Exception {
        Fixture fixture = fixture(true, true, List.of("736566774"));

        Object result = invoke(fixture.service(), "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotPrivateMessage")},
                message("885391366", "#群 736566774 状态"));

        assertThat(invoke(result, "handled")).isEqualTo(true);
        assertThat(invoke(result, "operation")).isEqualTo("STATUS");
        assertThat((String) invoke(invoke(result, "outboundMessage"), "text")).contains("group: 736566774");
        Mockito.verifyNoInteractions(fixture.adminOpLogService());
    }

    private Fixture fixture(boolean adminConfigured, boolean privateAdminEnabled, List<String> allowedGroups)
            throws Exception {
        return fixture(adminConfigured, privateAdminEnabled, allowedGroups, new AtomicReference<>());
    }

    private Fixture fixture(
            boolean adminConfigured,
            boolean privateAdminEnabled,
            List<String> allowedGroups,
            AtomicReference<Object> updatedConfig) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        invoke(properties, "setAdmins", new Class<?>[]{List.class},
                adminConfigured ? List.of("885391366") : List.of("20001"));
        Object onebot = invoke(properties, "getOnebot");
        invoke(onebot, "setAllowedGroupIds", new Class<?>[]{List.class}, allowedGroups);
        Object privateAdmin = invoke(properties, "getPrivateAdmin");
        invoke(privateAdmin, "setEnabled", new Class<?>[]{boolean.class}, privateAdminEnabled);
        Object replies = invoke(privateAdmin, "getReplies");
        invoke(replies, "setDisabled", new Class<?>[]{String.class}, "private disabled");
        invoke(replies, "setGroupNotAllowed", new Class<?>[]{String.class}, "group not allowed");
        invoke(replies, "setUnknownCommand", new Class<?>[]{String.class}, "unknown");
        invoke(replies, "setSuccess", new Class<?>[]{String.class}, "success");
        invoke(replies, "setStatusPrefix", new Class<?>[]{String.class}, "status");

        Object groupConfig = config();
        Object groupConfigService = groupConfigService(groupConfig, updatedConfig);
        Object adminOpLogService = mockClass("com.yh.qqbot.service.log.AdminOpLogService");
        Object groupAdminService = cls("com.yh.qqbot.service.admin.GroupConfigAdminService")
                .getConstructor(cls("com.yh.qqbot.service.config.GroupConfigService"))
                .newInstance(groupConfigService);
        Object service = cls("com.yh.qqbot.service.command.PrivateAdminCommandService")
                .getConstructor(
                        cls("com.yh.qqbot.config.properties.QqBotProperties"),
                        cls("com.yh.qqbot.service.admin.GroupConfigAdminService"),
                        cls("com.yh.qqbot.service.log.AdminOpLogService"),
                        cls("com.yh.qqbot.service.command.MemberRankCommandService"))
                .newInstance(
                        properties,
                        groupAdminService,
                        adminOpLogService,
                        mockClass("com.yh.qqbot.service.command.MemberRankCommandService"));
        return new Fixture(service, groupConfigService, adminOpLogService);
    }

    private Object groupConfigService(Object config, AtomicReference<Object> updatedConfig) throws Exception {
        Class<?> type = cls("com.yh.qqbot.service.config.GroupConfigService");
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if ("getConfig".equals(method.getName())) {
                return config;
            }
            if ("updateConfig".equals(method.getName())) {
                @SuppressWarnings("unchecked")
                UnaryOperator<Object> updater = (UnaryOperator<Object>) args[1];
                Object updated = updater.apply(config);
                updatedConfig.set(updated);
                return updated;
            }
            return null;
        });
    }

    private Object message(String userId, String text) throws Exception {
        return cls("com.yh.qqbot.dto.BotPrivateMessage")
                .getConstructor(String.class, String.class, String.class, String.class, Instant.class)
                .newInstance(userId, "private-msg-1", text, text, Instant.now());
    }

    private Object config() throws Exception {
        Object memoryMode = Enum.valueOf((Class<Enum>) cls("com.yh.qqbot.enums.MemoryMode"), "SHORT");
        return cls("com.yh.qqbot.dto.GroupConfigSnapshot")
                .getConstructor(
                        String.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        long.class,
                        long.class,
                        long.class,
                        String.class,
                        String.class,
                        String.class,
                        cls("com.yh.qqbot.enums.MemoryMode"),
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class)
                .newInstance(
                        "736566774",
                        true,
                        true,
                        true,
                        true,
                        false,
                        180L,
                        20L,
                        80L,
                        null,
                        "safe",
                        "",
                        memoryMode,
                        false,
                        false,
                        false,
                        false);
    }

    private Object mockClass(String className) throws Exception {
        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) cls(className);
        return Mockito.mock(type);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private record Fixture(
            Object service,
            Object groupConfigService,
            Object adminOpLogService) {
    }
}
