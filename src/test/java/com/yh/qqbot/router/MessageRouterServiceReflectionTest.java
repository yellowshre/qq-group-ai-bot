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
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        verifyNoInteractions(fixture.activeChatPolicyService());
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
        verifyNoInteractions(fixture.activeChatPolicyService());
    }

    @Test
    void normalMessageDoesNotCallDifyPassiveChat() throws Exception {
        Fixture fixture = fixture();
        Object message = message(false, false, "normal");
        stubNoMeme(fixture.memeMatchService(), "normal");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "passiveChatHit")).isEqualTo(false);
        verifyNoInteractions(fixture.difyWorkflowService());
        verifyNoInteractions(fixture.knowledgeContextService());
    }

    @Test
    void passiveChatKnowledgeEnabledPassesContextToDify() throws Exception {
        Fixture fixture = fixture(true, false, true, false, true, false);
        Object message = message(true, false, "hello knowledge");
        stubKnowledgeContext(fixture.knowledgeContextService(), "reviewed passive context");
        stubPassiveReplyWithKnowledge(
                fixture.difyWorkflowService(),
                "hello knowledge",
                "reviewed passive context",
                Optional.of(passiveReply("reply ok", 0.88)));
        stubNoMeme(fixture.memeMatchService(), "reply ok");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("PASSIVE_CHAT");
        assertThat(invoke(result, "passiveChatHit")).isEqualTo(true);
        assertThat(invoke(result, "replyText")).isEqualTo("reply ok");
        verifyPassiveReplyWithKnowledge(fixture.difyWorkflowService(), "reviewed passive context");
    }

    @Test
    void memeRouteWinsBeforeActiveChat() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "plain active text");
        stubMeme(fixture.memeMatchService(), "plain active text", 8L, "laugh", "C:/qqbot/memes/laugh.png");

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("MEME");
        assertThat(invoke(result, "memeHit")).isEqualTo(true);
        assertThat(invoke(result, "memeId")).isEqualTo(8L);
        verifyNoInteractions(fixture.activeChatPolicyService(), fixture.difyWorkflowService());
    }

    @Test
    void activeChatSuccessReturnsActiveChatResult() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubActiveReply(fixture.difyWorkflowService(), activeReply("active reply", 0.91));

        Object result = route(fixture.router(), message);
        Object outbound = invoke(result, "outboundMessage");

        assertThat(invoke(result, "routeType").toString()).isEqualTo("ACTIVE_CHAT");
        assertThat(invoke(result, "responseType")).isEqualTo("ACTIVE_CHAT");
        assertThat(invoke(result, "shouldSend")).isEqualTo(true);
        assertThat(invoke(result, "activeChatHit")).isEqualTo(true);
        assertThat(invoke(result, "activePolicyPassed")).isEqualTo(true);
        assertThat(invoke(result, "activeShouldReply")).isEqualTo(true);
        assertThat(invoke(result, "activeConfidence")).isEqualTo(0.91);
        assertThat(invoke(result, "workflowType")).isEqualTo("ACTIVE_DIFY_CHAT");
        assertThat(invoke(result, "replyText")).isEqualTo("active reply");
        assertThat(invoke(outbound, "text")).isEqualTo("active reply");
    }

    @Test
    void activeChatKnowledgeEnabledPassesContextToDifyRequest() throws Exception {
        Fixture fixture = fixture(true, true, true, false, false, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubKnowledgeContext(fixture.knowledgeContextService(), "reviewed active context");
        stubActiveReply(fixture.difyWorkflowService(), activeReply("active reply", 0.91));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("ACTIVE_CHAT");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass((Class<Object>) cls("com.yh.qqbot.dto.ActiveChatRequest"));
        Object verified = Mockito.verify(fixture.difyWorkflowService());
        invoke(verified, "generateActiveReply",
                new Class<?>[]{cls("com.yh.qqbot.dto.ActiveChatRequest")},
                requestCaptor.capture());
        assertThat(invoke(requestCaptor.getValue(), "knowledgeContext")).isEqualTo("reviewed active context");
    }

    @Test
    void activePolicyRejectedDoesNotCallDify() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyRejected("COOLDOWN"));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "activePolicyPassed")).isEqualTo(false);
        assertThat(invoke(result, "activePolicyRejectReason")).isEqualTo("COOLDOWN");
        assertThat(invoke(result, "silentReason")).isEqualTo("COOLDOWN");
        verifyNoInteractions(fixture.difyWorkflowService());
    }

    @Test
    void activeDifyShouldReplyFalseReturnsSilent() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubActiveReply(fixture.difyWorkflowService(), activeRejected("SHOULD_REPLY_FALSE", "", 0.72));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "activeChatHit")).isEqualTo(true);
        assertThat(invoke(result, "activePolicyPassed")).isEqualTo(true);
        assertThat(invoke(result, "activeShouldReply")).isEqualTo(false);
        assertThat(invoke(result, "silentReason")).isEqualTo("SHOULD_REPLY_FALSE");
    }

    @Test
    void activeDifyLowConfidenceReturnsSilent() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubActiveReply(fixture.difyWorkflowService(), activeReply("maybe reply", 0.2));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "activePolicyPassed")).isEqualTo(true);
        assertThat(invoke(result, "activeShouldReply")).isEqualTo(false);
        assertThat(invoke(result, "activeConfidence")).isEqualTo(0.2);
        assertThat(invoke(result, "silentReason")).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    void activeDifyExceptionReturnsSilent() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubActiveReplyThrows(fixture.difyWorkflowService(), new RuntimeException("boom"));

        Object result = route(fixture.router(), message);

        assertThat(invoke(result, "routeType").toString()).isEqualTo("SILENT");
        assertThat(invoke(result, "activePolicyPassed")).isEqualTo(true);
        assertThat(invoke(result, "activeShouldReply")).isEqualTo(false);
        assertThat(invoke(result, "silentReason")).isEqualTo("DIFY_ERROR");
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
    void afterSuccessfulActiveSendWritesContextPolicyAndTriggerLog() throws Exception {
        Fixture fixture = fixture(true, true);
        Object message = message(false, false, "ordinary active message");
        stubNoMeme(fixture.memeMatchService(), "ordinary active message");
        stubActivePolicy(fixture.activeChatPolicyService(), activePolicyAllowed());
        stubActiveReply(fixture.difyWorkflowService(), activeReply("active reply", 0.91));
        Object result = route(fixture.router(), message);

        invoke(fixture.router(), "afterSend",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.RouteResult"), boolean.class},
                message, result, true);

        assertThat(fixture.chatContext().userMessages()).contains("20001:ordinary active message");
        assertThat(fixture.chatContext().botReplies()).contains("小黄:active reply");
        verifyActiveChatMarked(fixture.activeChatPolicyService());
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
        return fixture(difyEnabled, false);
    }

    private Fixture fixture(boolean difyEnabled, boolean activeChatEnabled) throws Exception {
        return fixture(difyEnabled, activeChatEnabled, false, false, false, false);
    }

    private Fixture fixture(
            boolean difyEnabled,
            boolean activeChatEnabled,
            boolean enableKnowledgeContext,
            boolean enableMemeKnowledge,
            boolean enablePassiveChatKnowledge,
            boolean enableActiveChatKnowledge) throws Exception {
        Object messageDedupService = proxy("com.yh.qqbot.service.rate.MessageDedupService", (method, args) -> {
            if ("firstSeen".equals(method.getName())) {
                return true;
            }
            return defaultValue(method.getReturnType());
        });
        Object groupConfig = groupConfig(activeChatEnabled, enableKnowledgeContext,
                enableMemeKnowledge, enablePassiveChatKnowledge, enableActiveChatKnowledge);
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
        Object knowledgeContextService = mockClass("com.yh.qqbot.chat.history.service.context.KnowledgeContextService");
        Object properties = properties(difyEnabled);
        Object activeChatPolicyService = mockClass("com.yh.qqbot.service.active.ActiveChatPolicyService");
        Object botIdentityService = cls("com.yh.qqbot.service.bot.BotIdentityService")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
        Object botSafetyWordService = cls("com.yh.qqbot.service.bot.BotSafetyWordService")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
        stubActivePolicy(activeChatPolicyService, activePolicyRejected("GROUP_ACTIVE_CHAT_DISABLED"));

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
                        cls("com.yh.qqbot.service.active.ActiveChatPolicyService"),
                        cls("com.yh.qqbot.service.bot.BotIdentityService"),
                        cls("com.yh.qqbot.service.bot.BotSafetyWordService"),
                        cls("com.yh.qqbot.chat.history.service.context.KnowledgeContextService"),
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
                        activeChatPolicyService,
                        botIdentityService,
                        botSafetyWordService,
                        knowledgeContextService,
                        properties);

        whenInvoke(adminCommandService, "tryHandle",
                new Class<?>[]{cls("com.yh.qqbot.dto.BotGroupMessage"), cls("com.yh.qqbot.dto.GroupConfigSnapshot")},
                new Object[]{message(true, false, "unused"), groupConfig},
                notCommand());
        whenInvoke(groupPersonaService, "getPersona", new Class<?>[]{Long.class}, new Object[]{10001L}, "persona");
        Mockito.clearInvocations(activeChatPolicyService);

        return new Fixture(router, memeMatchService, difyWorkflowService, activeChatPolicyService,
                triggerLogService, knowledgeContextService, chatContextState);
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

    private void stubPassiveReplyWithKnowledge(
            Object dify,
            String text,
            String knowledgeContext,
            Optional<?> reply) throws Exception {
        Method method = dify.getClass().getMethod("generatePassiveReply",
                String.class, Long.class, Long.class, String.class, String.class, List.class, String.class);
        when(method.invoke(dify,
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(knowledgeContext))).thenReturn(reply);
    }

    private void verifyPassiveReplyWithKnowledge(Object dify, String knowledgeContext) throws Exception {
        Object verified = Mockito.verify(dify);
        invoke(verified, "generatePassiveReply",
                new Class<?>[]{String.class, Long.class, Long.class, String.class, String.class, List.class, String.class},
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(knowledgeContext));
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
    private void stubKnowledgeContext(Object knowledgeContextService, String context) throws Exception {
        Object result = cls("com.yh.qqbot.chat.history.service.context.KnowledgeContextService$KnowledgeContextBuildResult")
                .getConstructor(boolean.class, String.class, List.class)
                .newInstance(true, context, List.of());
        Method method = knowledgeContextService.getClass().getMethod("buildContext",
                String.class,
                String.class,
                String.class,
                cls("com.yh.qqbot.chat.history.dto.KnowledgeRouteType"),
                Integer.class);
        when(method.invoke(knowledgeContextService,
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(result);
    }

    private void stubActivePolicy(Object activeChatPolicyService, Object result) throws Exception {
        whenInvokeAny(activeChatPolicyService, "evaluate",
                new Class<?>[]{cls("com.yh.qqbot.dto.ActiveChatPolicyRequest")},
                result);
        Mockito.clearInvocations(activeChatPolicyService);
    }

    private void stubActiveReply(Object dify, Object result) throws Exception {
        whenInvokeAny(dify, "generateActiveReply",
                new Class<?>[]{cls("com.yh.qqbot.dto.ActiveChatRequest")},
                result);
        Mockito.clearInvocations(dify);
    }

    private void stubActiveReplyThrows(Object dify, RuntimeException exception) throws Exception {
        Method method = dify.getClass().getMethod("generateActiveReply", cls("com.yh.qqbot.dto.ActiveChatRequest"));
        when(method.invoke(dify, new Object[]{org.mockito.ArgumentMatchers.any()})).thenThrow(exception);
        Mockito.clearInvocations(dify);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void whenInvoke(Object mock, String methodName, Class<?>[] parameterTypes, Object[] args, Object result) throws Exception {
        Method method = mock.getClass().getMethod(methodName, parameterTypes);
        when(method.invoke(mock, args)).thenReturn(result);
    }

    private void whenInvokeAny(Object mock, String methodName, Class<?>[] parameterTypes, Object result) throws Exception {
        Method method = mock.getClass().getMethod(methodName, parameterTypes);
        when(method.invoke(mock, new Object[]{org.mockito.ArgumentMatchers.any()})).thenReturn(result);
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

    private Object groupConfig(boolean activeChatEnabled) throws Exception {
        return groupConfig(activeChatEnabled, false, false, false, false);
    }

    private Object groupConfig(
            boolean activeChatEnabled,
            boolean enableKnowledgeContext,
            boolean enableMemeKnowledge,
            boolean enablePassiveChatKnowledge,
            boolean enableActiveChatKnowledge) throws Exception {
        Object memoryMode = Enum.valueOf((Class<Enum>) cls("com.yh.qqbot.enums.MemoryMode"), "SHORT");
        Object config = cls("com.yh.qqbot.dto.GroupConfigSnapshot")
                .getConstructor(String.class, boolean.class, boolean.class, boolean.class,
                        String.class, String.class, String.class, cls("com.yh.qqbot.enums.MemoryMode"))
                .newInstance("10001", true, true, activeChatEnabled, null, "safe", "persona", memoryMode);
        config = invoke(config, "withEnableKnowledgeContext", new Class<?>[]{boolean.class}, enableKnowledgeContext);
        config = invoke(config, "withEnableMemeKnowledge", new Class<?>[]{boolean.class}, enableMemeKnowledge);
        config = invoke(config, "withEnablePassiveChatKnowledge", new Class<?>[]{boolean.class}, enablePassiveChatKnowledge);
        config = invoke(config, "withEnableActiveChatKnowledge", new Class<?>[]{boolean.class}, enableActiveChatKnowledge);
        return config;
    }

    private Object notCommand() throws Exception {
        return cls("com.yh.qqbot.dto.CommandHandleResult").getMethod("notCommand").invoke(null);
    }

    private Object passiveReply(String replyText, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.PassiveChatReply")
                .getConstructor(String.class, double.class)
                .newInstance(replyText, confidence);
    }

    private Object activePolicyAllowed() throws Exception {
        return cls("com.yh.qqbot.dto.ActiveChatPolicyResult")
                .getMethod("allowed", long.class, long.class)
                .invoke(null, 180L, 20L);
    }

    private Object activePolicyRejected(String reason) throws Exception {
        return cls("com.yh.qqbot.dto.ActiveChatPolicyResult")
                .getMethod("rejected", String.class, boolean.class, long.class, long.class)
                .invoke(null, reason, true, 180L, 20L);
    }

    private Object activeReply(String replyText, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.ActiveChatReplyResult")
                .getMethod("success", String.class, double.class)
                .invoke(null, replyText, confidence);
    }

    private Object activeRejected(String reason, String replyText, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.ActiveChatReplyResult")
                .getMethod("rejected", String.class, String.class, double.class)
                .invoke(null, reason, replyText, confidence);
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

    private void verifyActiveChatMarked(Object activeChatPolicyService) throws Exception {
        Object verified = Mockito.verify(activeChatPolicyService);
        invoke(verified, "markActiveChatSent", new Class<?>[]{Long.class, Long.class}, 10001L, 180L);
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
            Object activeChatPolicyService,
            Object triggerLogService,
            Object knowledgeContextService,
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
