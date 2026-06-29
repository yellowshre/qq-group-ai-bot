package com.yh.qqbot.service.meme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MemeServiceReflectionTest {

    @Test
    void keywordHitDoesNotCallDify() throws Exception {
        CacheState cache = new CacheState();
        cache.keywordCache.put("哈哈", List.of(1L));
        Object dify = difyMock();
        MatchFixture fixture = newMatchService(
                cache,
                List.of(material(1L, "哈哈,笑死", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                dify
        );

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "哈哈", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isTrue();
        assertThat(invoke(result, "memeId")).isEqualTo(1L);
        assertThat(invoke(result, "matchType")).isEqualTo("MEME_KEYWORD");
        verifyNoInteractions(dify);
    }

    @Test
    void keywordMissAndDifyDisabledReturnsEmptyResult() throws Exception {
        CacheState cache = new CacheState();
        MatchFixture fixture = newMatchService(cache, List.of(), disabledDifyService());

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "没有关键词", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isFalse();
        assertThat(invoke(result, "memeId")).isNull();
        assertThat(invoke(result, "missReason")).isEqualTo("meme not matched");
    }

    @Test
    void difySceneWithEnoughConfidenceReturnsMeme() throws Exception {
        CacheState cache = new CacheState();
        cache.sceneCache.put("laugh", List.of(1L));
        Object dify = difyMock();
        stubDifyScene(dify, "这也太好笑了吧", 100L, 200L, Optional.of(sceneDecision("laugh", 0.86)));
        MatchFixture fixture = newMatchService(
                cache,
                List.of(material(1L, "", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                dify
        );

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "这也太好笑了吧", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isTrue();
        assertThat(invoke(result, "matchType")).isEqualTo("MEME_DIFY_SCENE");
        assertThat(invoke(result, "sceneCode")).isEqualTo("laugh");
        assertThat(invoke(result, "confidence")).isEqualTo(0.86);
        assertThat(cache.semanticWrites).containsKey("这也太好笑了吧");
    }

    @Test
    void difySceneBelowThresholdReturnsSilentResult() throws Exception {
        CacheState cache = new CacheState();
        Object dify = difyMock();
        stubDifyScene(dify, "像是有点好笑", 100L, 200L, Optional.of(sceneDecision("laugh", 0.5)));
        MatchFixture fixture = newMatchService(
                cache,
                List.of(material(1L, "", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                dify
        );

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "像是有点好笑", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isFalse();
        assertThat(invoke(result, "matchType")).isEqualTo("MEME_DIFY_SCENE");
        assertThat(invoke(result, "sceneCode")).isEqualTo("laugh");
        assertThat(invoke(result, "confidence")).isEqualTo(0.5);
        assertThat(invoke(result, "missReason")).isEqualTo("confidence below threshold");
    }

    @Test
    void difyExceptionReturnsSilentResult() throws Exception {
        CacheState cache = new CacheState();
        Object dify = difyMock();
        stubDifyException(dify, "网络波动消息", 100L, 200L);
        MatchFixture fixture = newMatchService(cache, List.of(), dify);

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "网络波动消息", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isFalse();
        assertThat(invoke(result, "missReason")).isEqualTo("dify scene recognition failed");
    }

    @Test
    void semanticCacheHitDoesNotCallDifyAgain() throws Exception {
        CacheState cache = new CacheState();
        cache.semanticCache.put("这句已经缓存过了", semanticEntry("laugh", 0.91));
        cache.sceneCache.put("laugh", List.of(1L));
        Object dify = difyMock();
        MatchFixture fixture = newMatchService(
                cache,
                List.of(material(1L, "", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                dify
        );

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class}, "这句已经缓存过了", "100", "200");

        assertThat((Boolean) invoke(result, "matched")).isTrue();
        assertThat(invoke(result, "matchType")).isEqualTo("MEME_SEMANTIC_CACHE");
        verifyNoInteractions(dify);
    }

    @Test
    void memeKnowledgeContextCallsDifyWithContextAndSkipsSemanticCacheWrite() throws Exception {
        CacheState cache = new CacheState();
        cache.sceneCache.put("laugh", List.of(1L));
        Object dify = difyMock();
        stubDifySceneWithKnowledge(
                dify,
                "local phrase",
                100L,
                200L,
                "reviewed meme context",
                Optional.of(sceneDecision("laugh", 0.86)));
        MatchFixture fixture = newMatchService(
                cache,
                List.of(material(1L, "", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                dify
        );

        Object result = invoke(fixture.service(), "match",
                new Class<?>[]{String.class, String.class, String.class, Supplier.class},
                "local phrase", "100", "200", (Supplier<String>) () -> "reviewed meme context");

        assertThat((Boolean) invoke(result, "matched")).isTrue();
        assertThat(invoke(result, "matchType")).isEqualTo("MEME_DIFY_SCENE");
        assertThat(cache.semanticWrites).doesNotContainKey("local phrase");
    }

    @Test
    void weightedRandomReturnsEnabledMaterialWithoutThrowing() throws Exception {
        Object materialService = newMaterialService(List.of());
        Method weightedRandom = materialService.getClass().getMethod("weightedRandom", List.class);

        Optional<?> selected = (Optional<?>) weightedRandom.invoke(materialService, List.of(
                material(1L, "哈哈", "laugh", 0, true, "C:/qqbot/memes/laugh_01.png"),
                material(2L, "生气", "angry", null, true, "C:/qqbot/memes/angry_01.png")
        ));

        assertThat(selected).isPresent();
    }

    private MatchFixture newMatchService(CacheState cache, List<Object> materials, Object difyWorkflowService) throws Exception {
        Class<?> lookupClass = cls("com.yh.qqbot.service.meme.MemeCacheLookup");
        Class<?> materialServiceClass = cls("com.yh.qqbot.service.meme.MemeMaterialService");
        Class<?> sceneDictServiceClass = cls("com.yh.qqbot.service.meme.SceneDictService");
        Class<?> difyWorkflowServiceClass = cls("com.yh.qqbot.service.chat.DifyWorkflowService");
        Class<?> matchServiceClass = cls("com.yh.qqbot.service.meme.MemeMatchService");

        Object lookup = Proxy.newProxyInstance(
                lookupClass.getClassLoader(),
                new Class<?>[]{lookupClass},
                (proxy, method, args) -> cache.handle(method, args)
        );

        Object matchService = matchServiceClass
                .getConstructor(lookupClass, materialServiceClass, sceneDictServiceClass, difyWorkflowServiceClass)
                .newInstance(lookup, newMaterialService(materials), newSceneDictService(), difyWorkflowService);
        return new MatchFixture(matchService);
    }

    private Object newMaterialService(List<Object> materials) throws Exception {
        Class<?> mapperClass = cls("com.yh.qqbot.mapper.MemeMaterialMapper");
        Object mapper = Proxy.newProxyInstance(
                mapperClass.getClassLoader(),
                new Class<?>[]{mapperClass},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return materials;
                    }
                    if ("selectBatchIds".equals(method.getName())) {
                        Collection<?> ids = (Collection<?>) args[0];
                        return materials.stream()
                                .filter(material -> ids.contains(memeIdOf(material)))
                                .toList();
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        Class<?> serviceClass = cls("com.yh.qqbot.service.meme.MemeMaterialService");
        return serviceClass.getConstructor(mapperClass).newInstance(mapper);
    }

    private Object newSceneDictService() throws Exception {
        Class<?> mapperClass = cls("com.yh.qqbot.mapper.SceneDictMapper");
        Map<String, Object> scenes = Map.of(
                "laugh", scene("laugh", 0.75),
                "angry", scene("angry", 0.75),
                "confused", scene("confused", 0.75)
        );
        Object mapper = Proxy.newProxyInstance(
                mapperClass.getClassLoader(),
                new Class<?>[]{mapperClass},
                (proxy, method, args) -> {
                    if ("selectById".equals(method.getName())) {
                        return scenes.get(String.valueOf(args[0]));
                    }
                    if ("selectList".equals(method.getName())) {
                        return List.copyOf(scenes.values());
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        Class<?> serviceClass = cls("com.yh.qqbot.service.meme.SceneDictService");
        return serviceClass.getConstructor(mapperClass).newInstance(mapper);
    }

    private Object difyMock() throws Exception {
        @SuppressWarnings("unchecked")
        Class<Object> difyClass = (Class<Object>) cls("com.yh.qqbot.service.chat.DifyWorkflowService");
        return Mockito.mock(difyClass);
    }

    private Object disabledDifyService() throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object difyProperties = invoke(properties, "getDify");
        invoke(difyProperties, "setEnabled", new Class<?>[]{boolean.class}, false);
        return cls("com.yh.qqbot.service.chat.DifyWorkflowService")
                .getConstructor(cls("com.yh.qqbot.adapter.dify.DifyClient"), cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(null, properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubDifyScene(Object dify, String text, Long groupId, Long userId, Optional<?> decision) throws Exception {
        Method method = dify.getClass().getMethod("recognizeMemeScene", String.class, Long.class, Long.class);
        when((Optional) method.invoke(dify, text, groupId, userId)).thenReturn((Optional) decision);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubDifySceneWithKnowledge(
            Object dify,
            String text,
            Long groupId,
            Long userId,
            String knowledgeContext,
            Optional<?> decision) throws Exception {
        Method method = dify.getClass().getMethod("recognizeMemeScene",
                String.class, Long.class, Long.class, String.class);
        when((Optional) method.invoke(dify, text, groupId, userId, knowledgeContext)).thenReturn((Optional) decision);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubDifyException(Object dify, String text, Long groupId, Long userId) throws Exception {
        Method method = dify.getClass().getMethod("recognizeMemeScene", String.class, Long.class, Long.class);
        when((Optional) method.invoke(dify, text, groupId, userId)).thenThrow(new RuntimeException("timeout"));
    }

    private Object sceneDecision(String sceneCode, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.SceneDecision")
                .getConstructor(String.class, double.class)
                .newInstance(sceneCode, confidence);
    }

    private Object semanticEntry(String sceneCode, double confidence) throws Exception {
        return cls("com.yh.qqbot.dto.MemeSemanticCacheEntry")
                .getConstructor(String.class, double.class, Instant.class)
                .newInstance(sceneCode, confidence, Instant.now());
    }

    private Object scene(String sceneCode, double threshold) throws Exception {
        Object scene = cls("com.yh.qqbot.entity.SceneDictEntity").getConstructor().newInstance();
        set(scene, "sceneCode", sceneCode);
        set(scene, "sceneDesc", sceneCode);
        set(scene, "confidenceThreshold", BigDecimal.valueOf(threshold));
        return scene;
    }

    private static Object material(Long memeId, String keywords, String sceneCode, Integer weight,
                                   boolean enabled, String filePath) throws Exception {
        Object material = cls("com.yh.qqbot.entity.MemeMaterialEntity")
                .getConstructor()
                .newInstance();
        set(material, "memeId", memeId);
        set(material, "keywords", keywords);
        set(material, "sceneCode", sceneCode);
        set(material, "sceneDesc", sceneCode);
        set(material, "weight", weight);
        set(material, "enabled", enabled);
        set(material, "filePath", filePath);
        return material;
    }

    private static void set(Object target, String property, Object value) throws Exception {
        String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(setter) && method.getParameterCount() == 1) {
                method.invoke(target, value);
                return;
            }
        }
        throw new NoSuchMethodException(setter);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static Object memeIdOf(Object material) {
        try {
            return invoke(material, "getMemeId");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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

    private record MatchFixture(Object service) {
    }

    private static class CacheState {
        private final Map<String, List<Long>> keywordCache = new HashMap<>();
        private final Map<String, List<Long>> sceneCache = new HashMap<>();
        private final Map<String, Object> semanticCache = new HashMap<>();
        private final Map<String, Object> semanticWrites = new HashMap<>();

        private Object handle(Method method, Object[] args) {
            if ("findMemeIdsByKeyword".equals(method.getName())) {
                return Optional.ofNullable(keywordCache.get(String.valueOf(args[0])));
            }
            if ("findMemeIdsBySceneCode".equals(method.getName())) {
                return Optional.ofNullable(sceneCache.get(String.valueOf(args[0])));
            }
            if ("findSemanticScene".equals(method.getName())) {
                return Optional.ofNullable(semanticCache.get(String.valueOf(args[0])));
            }
            if ("cacheSemanticScene".equals(method.getName())) {
                semanticWrites.put(String.valueOf(args[0]), args[1]);
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }
}
