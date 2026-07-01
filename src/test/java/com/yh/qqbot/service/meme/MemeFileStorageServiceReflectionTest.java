package com.yh.qqbot.service.meme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class MemeFileStorageServiceReflectionTest {

    @Test
    void storesFileWithGeneratedRelativePath() throws Exception {
        Path baseDir = baseDir();
        Object service = service(baseDir);

        Object result = store(service, "laugh", png("mayu.png"));

        assertThat(invoke(result, "relativePath")).isEqualTo("laugh/laugh_001.png");
        assertThat(Files.exists(baseDir.resolve("laugh").resolve("laugh_001.png"))).isTrue();
        assertThat((String) invoke(result, "oneBotFile")).startsWith("file:");
    }

    @Test
    void usesNextAvailableIndex() throws Exception {
        Path baseDir = baseDir();
        Files.createDirectories(baseDir.resolve("laugh"));
        Files.writeString(baseDir.resolve("laugh").resolve("laugh_001.png"), "old");
        Object service = service(baseDir);

        Object result = store(service, "laugh", png("anything.png"));

        assertThat(invoke(result, "relativePath")).isEqualTo("laugh/laugh_002.png");
        assertThat(Files.exists(baseDir.resolve("laugh").resolve("laugh_002.png"))).isTrue();
    }

    @Test
    void rejectsInvalidSceneCode() throws Exception {
        Object service = service(baseDir());

        assertThatThrownBy(() -> store(service, "../laugh", png("mayu.png")))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private Path baseDir() throws Exception {
        Path path = Path.of("target", "meme-upload-test", UUID.randomUUID().toString()).toAbsolutePath();
        Files.createDirectories(path);
        return path;
    }

    private Object service(Path baseDir) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        Object meme = properties.getClass().getMethod("getMeme").invoke(properties);
        meme.getClass().getMethod("setBaseDir", String.class).invoke(meme, baseDir.toString());
        return cls("com.yh.qqbot.service.meme.MemeFileStorageService")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
    }

    private Object store(Object service, String sceneCode, MultipartFile file) throws Exception {
        return service.getClass().getMethod("store", String.class, MultipartFile.class)
                .invoke(service, sceneCode, file);
    }

    private MultipartFile png(String filename) {
        return new MockMultipartFile("file", filename, "image/png", new byte[]{1, 2, 3});
    }

    private Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
