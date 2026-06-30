package com.yh.qqbot.adapter.onebot;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.net.URI;
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
        ImagePathInspection inspection = inspect(imagePath);
        if (!hasText(inspection.oneBotFile())) {
            return "";
        }
        if (Boolean.FALSE.equals(inspection.exists()) && inspection.checkable()) {
            log.warn("Meme image file does not exist. imagePath={}, resolvedPath={}",
                    imagePath, inspection.resolvedPath());
        }
        if (!inspection.checkable() && hasText(inspection.warning())) {
            log.warn("Meme image file cannot be checked. imagePath={}, resolvedPath={}, warning={}",
                    imagePath, inspection.resolvedPath(), inspection.warning());
        }
        return inspection.oneBotFile();
    }

    public ImagePathInspection inspect(String imagePath) {
        if (!hasText(imagePath)) {
            return new ImagePathInspection(imagePath, "", "", null, false, false, "empty image path");
        }
        String trimmed = imagePath.strip();
        if (isDirectReference(trimmed)) {
            return inspectDirectReference(trimmed);
        }

        if (isWindowsAbsolutePath(trimmed)) {
            Path windowsPath = Path.of(trimmed);
            if (windowsPath.isAbsolute()) {
                Path normalized = windowsPath.normalize();
                return inspectPath(imagePath, normalized, normalized.toUri().toString());
            }
            String normalized = trimmed.replace("\\", "/");
            return new ImagePathInspection(
                    imagePath,
                    "file:///" + normalized,
                    normalized,
                    null,
                    false,
                    false,
                    "windows absolute path cannot be checked on current OS");
        }

        Path resolved = resolvePath(trimmed);
        return inspectPath(imagePath, resolved, resolved.toUri().toString());
    }

    private ImagePathInspection inspectDirectReference(String value) {
        String lower = value.toLowerCase();
        if (lower.startsWith("file://")) {
            try {
                Path path = Path.of(URI.create(value)).normalize();
                return inspectPath(value, path, value);
            } catch (Exception ex) {
                return new ImagePathInspection(value, value, value, null, true, false, "invalid file uri");
            }
        }
        return new ImagePathInspection(value, value, value, null, true, false, "remote or inline image reference");
    }

    private ImagePathInspection inspectPath(String originalPath, Path resolvedPath, String oneBotFile) {
        try {
            return new ImagePathInspection(
                    originalPath,
                    oneBotFile,
                    resolvedPath.toString(),
                    Files.exists(resolvedPath),
                    false,
                    true,
                    null);
        } catch (Exception ex) {
            return new ImagePathInspection(
                    originalPath,
                    oneBotFile,
                    resolvedPath.toString(),
                    null,
                    false,
                    false,
                    ex.getMessage());
        }
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

    public record ImagePathInspection(
            String imagePath,
            String oneBotFile,
            String resolvedPath,
            Boolean exists,
            boolean directReference,
            boolean checkable,
            String warning) {
    }
}
