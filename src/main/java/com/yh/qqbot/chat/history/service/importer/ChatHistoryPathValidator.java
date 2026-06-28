package com.yh.qqbot.chat.history.service.importer;

import com.yh.qqbot.chat.history.service.InvalidChatHistoryFilePathException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryPathValidator {

    private static final String ILLEGAL_WINDOWS_CHARS = "?*<>|\":";

    public Path resolveAllowedPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new InvalidChatHistoryFilePathException();
        }
        if (containsIllegalPathChar(filePath)) {
            throw new InvalidChatHistoryFilePathException();
        }

        try {
            Path root = Paths.get("").toAbsolutePath().normalize();
            Path allowedRoot = root.resolve("data").resolve("chat-export").normalize();
            Path requested = Paths.get(filePath);
            if (requested.isAbsolute()) {
                throw new InvalidChatHistoryFilePathException();
            }

            Path resolved = root.resolve(requested).normalize();
            if (!resolved.startsWith(allowedRoot)) {
                throw new InvalidChatHistoryFilePathException();
            }
            if (!Files.isRegularFile(resolved)) {
                throw new InvalidChatHistoryFilePathException();
            }
            return resolved;
        } catch (InvalidPathException ex) {
            throw new InvalidChatHistoryFilePathException();
        }
    }

    private boolean containsIllegalPathChar(String filePath) {
        for (int i = 0; i < filePath.length(); i++) {
            char ch = filePath.charAt(i);
            if (ILLEGAL_WINDOWS_CHARS.indexOf(ch) >= 0) {
                return true;
            }
        }
        return false;
    }
}
