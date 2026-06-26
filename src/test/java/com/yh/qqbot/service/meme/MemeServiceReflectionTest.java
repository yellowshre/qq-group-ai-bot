package com.yh.qqbot.service.meme;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemeServiceReflectionTest {

    @Test
    void keywordHitReturnsMemeMatchResult() throws Exception {
        Object matchService = newMatchService(
                List.of(material(1L, "哈哈,笑死", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                Map.of("哈哈", List.of(1L))
        );

        Object result = invoke(matchService, "match", new Class<?>[]{String.class, String.class}, "哈哈", "200");

        assertThat((Boolean) invoke(result, "matched")).isTrue();
        assertThat(invoke(result, "memeId")).isEqualTo(1L);
        assertThat(invoke(result, "filePath")).isEqualTo("C:/qqbot/memes/laugh_01.png");
        assertThat(invoke(result, "matchType")).isEqualTo("keyword");
    }

    @Test
    void keywordMissReturnsEmptyResult() throws Exception {
        Object matchService = newMatchService(
                List.of(material(1L, "哈哈", "laugh", 10, true, "C:/qqbot/memes/laugh_01.png")),
                Map.of()
        );

        Object result = invoke(matchService, "match", new Class<?>[]{String.class, String.class}, "不存在", "200");

        assertThat((Boolean) invoke(result, "matched")).isFalse();
        assertThat(invoke(result, "memeId")).isNull();
    }

    @Test
    void weightedRandomReturnsEnabledMaterialWithoutThrowing() throws Exception {
        Object materialService = newMaterialService(List.of(
                material(1L, "哈哈", "laugh", 0, true, "C:/qqbot/memes/laugh_01.png"),
                material(2L, "生气", "angry", null, true, "C:/qqbot/memes/angry_01.png")
        ));
        Method weightedRandom = materialService.getClass().getMethod("weightedRandom", List.class);

        Optional<?> selected = (Optional<?>) weightedRandom.invoke(materialService, List.of(
                material(1L, "哈哈", "laugh", 0, true, "C:/qqbot/memes/laugh_01.png"),
                material(2L, "生气", "angry", null, true, "C:/qqbot/memes/angry_01.png")
        ));

        assertThat(selected).isPresent();
    }

    private Object newMatchService(List<Object> materials, Map<String, List<Long>> keywordCache) throws Exception {
        Class<?> lookupClass = Class.forName("com.yh.qqbot.service.meme.MemeCacheLookup");
        Class<?> materialServiceClass = Class.forName("com.yh.qqbot.service.meme.MemeMaterialService");
        Class<?> matchServiceClass = Class.forName("com.yh.qqbot.service.meme.MemeMatchService");

        Object lookup = Proxy.newProxyInstance(
                lookupClass.getClassLoader(),
                new Class<?>[]{lookupClass},
                (proxy, method, args) -> {
                    if ("findMemeIdsByKeyword".equals(method.getName())) {
                        return Optional.ofNullable(keywordCache.get(String.valueOf(args[0])));
                    }
                    if ("findMemeIdsBySceneCode".equals(method.getName())) {
                        return Optional.empty();
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        return matchServiceClass.getConstructor(lookupClass, materialServiceClass)
                .newInstance(lookup, newMaterialService(materials));
    }

    private Object newMaterialService(List<Object> materials) throws Exception {
        Class<?> mapperClass = Class.forName("com.yh.qqbot.mapper.MemeMaterialMapper");
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

        Class<?> serviceClass = Class.forName("com.yh.qqbot.service.meme.MemeMaterialService");
        return serviceClass.getConstructor(mapperClass).newInstance(mapper);
    }

    private static Object material(Long memeId, String keywords, String sceneCode, Integer weight,
                                   boolean enabled, String filePath) throws Exception {
        Object material = Class.forName("com.yh.qqbot.entity.MemeMaterialEntity")
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
}
