package com.yh.qqbot.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MessageRouterServiceReflectionTest {

    @Test
    void atBotTrueRoutesToPassiveDifyChat() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.of(passiveReply("reply ok", 0.88)));
        stubNoMeme(fixture.memeMatchService(), "reply ok");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("PASSIVE_CHAT");
        assertThat(invoke(result, "passiveChatHit")).isEqualTo(true);
        assertThat(invoke(result, "replyText")).isEqualTo("reply ok");
        assertThat(invoke(result, "chatConfidence")).isEqualTo(0.88);
        assertThat(invoke(result, "workflowType")).isEqualTo("PASSIVE_DIFY_CHAT");
        assertThat(invoke(result, "shouldSend")).isEqualTo(true);
    }

    @Test
    void botNicknameMatchedRoutesToPassiveDifyChat() throws Exception {
        Fixture fixture = fixture();
        Object message = message(false, true, "bot hi");
        stubPassiveReply(fixture.difyWorkflowService(), "bot hi", Optional.of(passiveReply("reply ok", 0.88)));
        stubNoMeme(fixture.memeMatchService(), "reply ok");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("PASSIVE_CHAT");
        assertThat(invoke(result, "passiveChatHit")).isEqualTo(true);
    }

    @Test
    void normalMessageDoesNotCallDifyPassiveChat() throws Exception {
        Fixture fixture = fixture();
        Object message = message(false, false, "normal");
        stubNoMeme(fixture.memeMatchService(), "normal");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "passiveChatHit")).isEqualTo(false);
        verifyNoInteractions(fixture.difyWorkflowService());
    }

    @Test
    void passiveReplyWithMemeKeepsReplyAndMemeMetadata() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "funny");
        stubPassiveReply(fixture.difyWorkflowService(), "funny", Optional.of(passiveReply("laugh reply", 0.9)));
        stubMeme(fixture.memeMatchService(), "laugh reply", 7L, "laugh", "C:/qqbot/memes/laugh.png");

        Object result = route(fixture.router(), message);
        Object outbound = invoke(result, "outboundMessage");

        assertThat(invoke(result, "memeHit")).isEqualTo(true);
        assertThat(invoke(result, "memeId")).isEqualTo(7L);
        assertThat(invoke(result, "sceneCode")).isEqualTo("laugh");
        assertThat(invoke(result, "workflowType")).isEqualTo("PASSIVE_DIFY_CHAT");
        assertThat(invoke(outbound, "text")).isEqualTo("laugh reply");
        assertThat(invoke(outbound, "imagePath")).isEqualTo("C:/qqbot/memes/laugh.png");
    }

    @Test
    void difyEmptyReturnsSilentReason() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.empty());

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "passiveChatHit")).isEqualTo(true);
        assertThat(invoke(result, "silentReason")).isEqualTo("passive chat unavailable");
    }

    @Test
    void difyDisabledReturnsExplicitSilentReason() throws Exception {
        Fixture fixture = fixture(false);
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.empty());

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "passiveChatHit")).isEqualTo(true);
        assertThat(invoke(result, "silentReason")).isEqualTo("dify disabled");
    }

    @Test
    void emptyReplyReturnsSilent() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.of(passiveReply("", 0.9)));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "silentReason")).isEqualTo("passive reply empty");
    }

    @Test
    void lowConfidenceReturnsSilent() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.of(passiveReply("reply", 0.2)));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "silentReason")).isEqualTo("passive confidence low");
        assertThat(invoke(result, "chatConfidence")).isEqualTo(0.2);
    }

    @Test
    void afterSuccessfulPassiveSendWritesContextAndTriggerLog() throws Exception {
        Fixture fixture = fixture();
        Object message = message(true, false, "hi");
        stubPassiveReply(fixture.difyWorkflowService(), "hi", Optional.of(passiveReply("reply ok", 0.88)));
        stubNoMeme(fixture.memeMatchService(), "reply ok");
        Object result = route(fixture.router(), message);

        invoke(fixture.router(), "afterSend",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.RouteResult"), boolean.class},
                message, result, true);

        assertThat(fixture.chatContext().userMessages()).contains("20001:hi");
        assertThat(fixture.chatContext().botReplies()).contains("小黄:reply ok");
        verifyTriggerLogRecorded(fixture.triggerLogService(), message, result);
    }

    @Test
    void inboundAdapterSendsTextBeforeImageWhenOutboundContainsBoth() throws Exception {
        Object message = message(true, false, "hi");
        Object router = mockClass("com.yh.qqbot.router.MessageRouterService");
        Object result = passiveTextWithImageResult("reply", "C:/qqbot/memes/laugh.png");
        whenInvoke(router, "route",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage")},
                new Object[]{message},
                result);
        List<String> sent = new ArrayList<>();
        Object sender = proxy("com.yh.qqbot.adapter.onebot.QqMessageSender", (method, args) -> {
            if ("sendGroupMessage".equals(method.getName())) {
                Object outbound = args[1];
                Object text = invoke(outbound, "text");
                Object image = invoke(outbound, "imagePath");
                sent.add(text != null ? "text:" + text : "image:" + image);
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        Object adapter = cls("com.yh.qqbot.adapter.onebot.OneBotInboundAdapter")
                .getConstructor(cls("com.yh.qqbot.router.MessageRouterService"), cls("com.yh.qqbot.adapter.onebot.QqMessageSender"))
                .newInstance(router, sender);

        invoke(adapter, "handleGroupMessage", new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage")}, message);

        assertThat(sent).containsExactly("text:reply", "image:C:/qqbot/memes/laugh.png");
    }

    private Fixture fixture() throws Exception {
        return fixture(true);
    }

    private Fixture fixture(boolean difyEnabled) throws Exception {
        Object messageDedupService = proxy("com.yh.qqbot.service.rate.MessageDedupService", (method, args) -> {
            if ("firstSeen".equals(method.getName())) {
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        Object groupConfig = groupConfig();
        Object groupConfigService = proxy("com.yh.qqbot.service.config.GroupConfigService", (method, args) -> {
            if ("getConfig".equals(method.getName())) {
                return groupConfig;
            }
            return groupConfig;
        });
        Object rateLimitService = proxy("com.yh.qqbot.service.rate.RateLimitService", (method, args) -> {
            if (method.getReturnType() == boolean.class) {
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        ChatContextState chatContextState = new ChatContextState();
        Object chatContextService = chatContextProxy(chatContextState);
        Object adminCommandService = mockClass("com.yh.qqbot.service.command.AdminCommandService");
        Object activeChatDecisionService = mockClass("com.yh.qqbot.service.active.ActiveChatDecisionService");
        Object chatReplyService = mockClass("com.yh.qqbot.service.chat.ChatReplyService");
        Object memeMatchService = mockClass("com.yh.qqbot.service.meme.MemeMatchService");
        Object triggerLogService = mockClass("com.yh.qqbot.service.log.TriggerLogService");
        Object groupPersonaService = mockClass("com.yh.qqbot.service.chat.GroupPersonaService");
        Object difyWorkflowService = mockClass("com.yh.qqbot.service.chat.DifyWorkflowService");
        Object properties = properties(difyEnabled);

        Object router = cls("com.yh.qqbot.router.MessageRouterService")
                .getConstructor(
                        cls("com.yh.qqbot.service.rate.MessageDedupService"),
                        cls("com.yh.qqbot.service.config.GroupConfigService"),
                        cls("com.yh.qqbot.service.command.AdminCommandService"),
                        cls("com.yh.qqbot.service.active.ActiveChatDecisionService"),
                        cls("com.yh.qqbot.service.chat.ChatReplyService"),
                        cls("com.yh.qqbot.service.meme.MemeMatchService"),
                        cls("com.yh.qqbot.service.rate.RateLimitService"),
                        cls("com.yh.qqbot.service.context.ChatContextService"),
                        cls("com.yh.qqbot.service.log.TriggerLogService"),
                        cls("com.yh.qqbot.service.chat.GroupPersonaService"),
                        cls("com.yh.qqbot.service.chat.DifyWorkflowService"),
                        cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(
                        messageDedupService,
                        groupConfigService,
                        adminCommandService,
                        activeChatDecisionService,
                        chatReplyService,
                        memeMatchService,
                        rateLimitService,
                        chatContextService,
                        triggerLogService,
                        groupPersonaService,
                        difyWorkflowService,
                        properties);

        whenInvoke(adminCommandService, "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.GroupConfigSnapshot")},
                new Object[]{message(true, false, "unused"), groupConfig},
                notCommand());
        whenInvoke(groupPersonaService, "getPersona", new Class<?>[]{Long.class}, new Object[]{10001L}, "persona");

        return new Fixture(router, memeMatchService, difyWorkflowService, triggerLogService, chatContextState);
    }

    private Object route(Object router, Object message) throws Exception {
        stubAdminForMessage(router, message);
        return invoke(router, "route", new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage")}, message);
    }

    private void stubAdminForMessage(Object router, Object message) throws Exception {
        Object admin = field(router, "adminCommandService");
        Object groupConfigService = field(router, "groupConfigService");
        Object config = invoke(groupConfigService, "getConfig", new Class<?>[]{String.class}, "10001");
        whenInvoke(admin, "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.GroupConfigSnapshot")},
                new Object[]{message, config},
                notCommand());
    }

    private void stubPassiveReply(Object dify, String text, Optional<?> reply) throws Exception {
        whenInvoke(dify, "generatePassiveReply",
                new Class<?>[]{String.class, Long.class, Long.class, String.class, String.class, List.class},
                new Object[]{text, 10001L, 20001L, "小黄", "persona", List.of("ctx")},
                reply);
    }

    private void stubNoMeme(Object memeMatchService, String text) throws Exception {
        whenInvoke(memeMatchService, "match",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{text, "10001", "20001"},
                memeEmpty());
    }

    private void stubMeme(Object memeMatchService, String text, Long memeId, String sceneCode, String filePath) throws Exception {
        whenInvoke(memeMatchService, "match",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{text, "10001", "20001"},
                memeResult(memeId, sceneCode, filePath));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void whenInvoke(Object mock, String methodName, Class<?>[] parameterTypes, Object[] args, Object result) throws Exception {
        Method method = mock.getClass().getMethod(methodName, parameterTypes);
        when(method.invoke(mock, args)).thenReturn(result);
    }

    private Object chatContextProxy(ChatContextState state) throws Exception {
        return proxy("com.yh.qqbot.service.context.ChatContextService", (method, args) -> {
            return switch (method.getName()) {
                case "getRecentMessages" -> "ctx";
                case "appendUserMessage" -> {
                    state.userMessages.add(args[1] + ":" + args[2]);
                    yield null;
                }
                case "appendBotReply" -> {
                    state.botReplies.add(args[1] + ":" + args[2]);
                    yield null;
                }
                case "recentMessagesForActiveDecision", "loadHotContext", "loadColdSummaries" -> List.of();
                default -> defaultValue(method.getReturnType());
            };
        });
    }

    private Object properties(boolean difyEnabled) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object dify = invoke(properties, "getDify");
        invoke(dify, "setEnabled", new Class<?>[]{boolean.class}, difyEnabled);
        invoke(dify, "setPassiveChatMinConfidence", new Class<?>[]{double.class}, 0.5);
        return properties;
    }

    private Object groupConfig() throws Exception {
        Object memoryMode = Enum.valueOf((Class<Enum>) cls("com.yh.qqbot.enums.MemoryMode"), "SHORT");
        return cls("com.yh.qqbot.dto.GroupConfigSnapshot")
                .getConstructor(String.class, boolean.class, boolean.class, boolean.class,
                        String.class, String.class, String.class, cls("com.yh.qqbot.enums.MemoryMode"))
                .newInstance("10001", true, true, false, null, "safe", "persona", memoryMode);
    }

    private Object notCommand() throws Exception {
        return cls("com.yh.qqbot.dto.CommandHandleResult").getMethod("notCommand").invoke(null);
    }

    private Object passiveReply(String replyText, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.PassiveChatReply")
                .getConstructor(String.class, double.class)
                .newInstance(replyText, confidence);
    }

    private Object memeEmpty() throws Exception {
        return cls("com.yh.qqbot.dto.MemeMatchResult").getMethod("empty").invoke(null);
    }

    private Object memeResult(Long memeId, String sceneCode, String filePath) throws Exception {
        return cls("com.yh.qqbot.dto.MemeMatchResult")
                .getConstructor(Long.class, String.class, String.class, String.class, Double.class, String.class)
                .newInstance(memeId, sceneCode, filePath, "MEME_KEYWORD", null, null);
    }

    private Object passiveTextWithImageResult(String text, String imagePath) throws Exception {
        Object routeType = Enum.valueOf((Class<Enum>) cls("com.yh.qqbot.enums.RouteType"), "PASSIVE_CHAT");
        Object outbound = cls("com.yh.qqbot.dto.OutboundMessage")
                .getMethod("textWithImage", String.class, String.class)
                .invoke(null, text, imagePath);
        Object result = cls("com.yh.qqbot.dto.RouteResult")
                .getMethod("send", cls("com.yh.qqbot.enums.RouteType"), cls("com.yh.qqbot.dto.OutboundMessage"), String.class)
                .invoke(null, routeType, outbound, "passive chat reply generated");
        return invoke(result, "withWorkflowType", new Class<?>[]{String.class}, "PASSIVE_DIFY_CHAT");
    }

    private Object message(boolean atBot, boolean nicknameMatched, String text) throws Exception {
        return cls("com.yh.qqbot.dto.BotGroupMessage")
                .getConstructor(String.class, String.class, String.class, String.class, String.class,
                        boolean.class, boolean.class, Instant.class)
                .newInstance("10001", "20001", "msg-" + text + "-" + atBot + "-" + nicknameMatched,
                        text, text, atBot, nicknameMatched, Instant.now());
    }

    private void verifyTriggerLogRecorded(Object triggerLog, Object message, Object result) throws Exception {
        Object verified = Mockito.verify(triggerLog);
        invoke(verified, "record",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.RouteResult"), boolean.class, String.class},
                message, result, true, null);
    }

    private Object proxy(String className, Invocation invocation) throws Exception {
        Class<?> type = cls(className);
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> invocation.handle(method, args));
    }

    private Object mockClass(String className) throws Exception {
        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) cls(className);
        return Mockito.mock(type);
    }

    private Object field(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == void.class) {
            return null;
        }
        return 0;
    }

    private interface Invocation {
        Object handle(Method method, Object[] args) throws Throwable;
    }

    private record Fixture(
            Object router,
            Object memeMatchService,
            Object difyWorkflowService,
            Object triggerLogService,
            ChatContextState chatContext) {
    }

    private static class ChatContextState {
        private final List<String> userMessages = new ArrayList<>();
        private final List<String> botReplies = new ArrayList<>();

        List<String> userMessages() {
            return userMessages;
        }

        List<String> botReplies() {
            return botReplies;
        }
    }
}
