package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OneBotImagePathResolver {

    private static final Logger log = LoggerFactory.getLogger(OneBotImagePathResolver.class);
    private static final String DEFAULT_BASE_DIR = "./memes";

    private final QqBotProperties properties;

    public OneBotImagePathResolver(QqBotProperties properties) {
        this.properties = properties;
    }

    public String toOneBotFile(String imagePath) {
        if (!hasText(imagePath)) {
            return "";
        }

        String trimmed = imagePath.strip();
        if (isDirectReference(trimmed)) {
            return trimmed;
        }

        if (isWindowsAbsolutePath(trimmed)) {
            Path windowsPath = Path.of(trimmed);
            if (windowsPath.isAbsolute()) {
                Path normalized = windowsPath.normalize();
                warnIfMissing(imagePath, normalized);
                return normalized.toUri().toString();
            }
            String normalized = trimmed.replace("\\", "/");
            log.warn("Meme image file does not exist or cannot be checked on this OS. imagePath={}, resolvedPath={}",
                    imagePath, normalized);
            return "file:///" + normalized;
        }

        Path resolved = resolvePath(trimmed);
        warnIfMissing(imagePath, resolved);
        return resolved.toUri().toString();
    }

    private Path resolvePath(String imagePath) {
        Path path = Path.of(imagePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return memeBaseDir().resolve(path).normalize();
    }

    private Path memeBaseDir() {
        String baseDir = properties.getMeme() == null ? null : properties.getMeme().getBaseDir();
        if (!hasText(baseDir)) {
            baseDir = DEFAULT_BASE_DIR;
        }
        Path path = Path.of(baseDir.strip());
        return path.isAbsolute() ? path.normalize() : path.toAbsolutePath().normalize();
    }

    private void warnIfMissing(String originalPath, Path resolvedPath) {
        try {
            if (Files.notExists(resolvedPath)) {
                log.warn("Meme image file does not exist. imagePath={}, resolvedPath={}", originalPath, resolvedPath);
            }
        } catch (Exception ex) {
            log.warn("Unable to check meme image file. imagePath={}, resolvedPath={}", originalPath, resolvedPath, ex);
        }
    }

    private boolean isDirectReference(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("file://")
                || lower.startsWith("base64://");
    }

    private boolean isWindowsAbsolutePath(String value) {
        return value.length() > 2
                && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':'
                && (value.charAt(2) == '\\' || value.charAt(2) == '/');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
