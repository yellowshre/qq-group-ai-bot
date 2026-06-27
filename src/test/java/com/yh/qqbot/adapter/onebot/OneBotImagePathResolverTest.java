package com.yh.qqbot.adapter.onebot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OneBotImagePathResolverTest {

    @Test
    void resolvesRelativeMemePathAgainstConfiguredBaseDir() throws Exception {
        Path baseDir = Path.of("target", "test-memes", UUID.randomUUID().toString()).toAbsolutePath();
        Path image = baseDir.resolve("laugh").resolve("laugh_001.png");
        Files.createDirectories(image.getParent());
        Files.createFile(image);

        Object resolver = resolver(baseDir.toString());

        assertThat(toOneBotFile(resolver, "laugh/laugh_001.png"))
                .isEqualTo(image.toUri().toString());
    }

    @Test
    void keepsExistingFileUri() throws Exception {
        Object resolver = resolver("");

        assertThat(toOneBotFile(resolver, "file:///C:/qqbot/memes/laugh_001.png"))
                .isEqualTo("file:///C:/qqbot/memes/laugh_001.png");
    }

    private Object resolver(String baseDir) throws Exception {
        Object properties = cls("com.yh.qqbot.config.properties.QqBotProperties").getConstructor().newInstance();
        if (baseDir != null && !baseDir.isBlank()) {
            Object meme = properties.getClass().getMethod("getMeme").invoke(properties);
            meme.getClass().getMethod("setBaseDir", String.class).invoke(meme, baseDir);
        }
        return cls("com.yh.qqbot.adapter.onebot.OneBotImagePathResolver")
                .getConstructor(cls("com.yh.qqbot.config.properties.QqBotProperties"))
                .newInstance(properties);
    }

    private String toOneBotFile(Object resolver, String imagePath) throws Exception {
        return (String) resolver.getClass().getMethod("toOneBotFile", String.class).invoke(resolver, imagePath);
    }

    private Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
