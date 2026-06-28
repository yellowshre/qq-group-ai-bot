package com.yh.qqbot.chat.history.service.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChatHistoryPathValidatorTest {

    @Test
    void rejectsIllegalWindowsPathCharacters() throws Exception {
        Throwable thrown = thrownByResolve("data/chat-export/group_?????_251288204.json");

        assertThat(thrown).isInstanceOf(cls("com.yh.qqbot.chat.history.service.InvalidChatHistoryFilePathException"));
        assertThat(thrown).hasMessage("Invalid chat history file path");
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        Throwable thrown = thrownByResolve("data/chat-export/../secret.json");

        assertThat(thrown).isInstanceOf(cls("com.yh.qqbot.chat.history.service.InvalidChatHistoryFilePathException"));
    }

    @Test
    void acceptsAsciiFileUnderChatExportDirectory() throws Exception {
        Path file = Path.of("data", "chat-export", "group_251288204_sample_20260628_185926.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}", StandardCharsets.UTF_8);
        try {
            Path resolved = resolve("data/chat-export/group_251288204_sample_20260628_185926.json");

            assertThat(resolved).isRegularFile();
            assertThat(resolved.toAbsolutePath().normalize().toString()).endsWith("group_251288204_sample_20260628_185926.json");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private Throwable thrownByResolve(String filePath) throws Exception {
        try {
            resolve(filePath);
            return null;
        } catch (InvocationTargetException ex) {
            return ex.getCause();
        }
    }

    private Path resolve(String filePath) throws Exception {
        Object validator = cls("com.yh.qqbot.chat.history.service.importer.ChatHistoryPathValidator")
                .getConstructor()
                .newInstance();
        return (Path) validator.getClass()
                .getMethod("resolveAllowedPath", String.class)
                .invoke(validator, filePath);
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}
