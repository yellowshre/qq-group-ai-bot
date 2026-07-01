package com.yh.qqbot.service.meme;

import com.yh.qqbot.config.properties.QqBotProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MemeFileStorageService {

    private static final String DEFAULT_BASE_DIR = "./memes";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;

    private final QqBotProperties properties;

    public MemeFileStorageService(QqBotProperties properties) {
        this.properties = properties;
    }

    public UploadResult store(String sceneCode, MultipartFile file) {
        String normalizedSceneCode = normalizeSceneCode(sceneCode);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("meme image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("meme image file is too large");
        }
        String extension = extension(file.getOriginalFilename());

        Path baseDir = memeBaseDir();
        Path sceneDir = baseDir.resolve(normalizedSceneCode).normalize();
        ensureInside(baseDir, sceneDir, "scene directory must stay inside meme base dir");

        try {
            Files.createDirectories(sceneDir);
            Path target = nextAvailableTarget(sceneDir, normalizedSceneCode, extension);
            ensureInside(sceneDir, target, "target file must stay inside scene directory");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target);
            }
            String fileName = target.getFileName().toString();
            return new UploadResult(
                    normalizedSceneCode + "/" + fileName,
                    fileName,
                    target.toString(),
                    target.toUri().toString(),
                    file.getSize(),
                    safeText(file.getContentType()));
        } catch (FileAlreadyExistsException ex) {
            throw new IllegalStateException("meme image file already exists, please retry", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to store meme image file", ex);
        }
    }

    private Path nextAvailableTarget(Path sceneDir, String sceneCode, String extension) throws IOException {
        for (int index = 1; index <= 9999; index++) {
            Path target = sceneDir.resolve("%s_%03d.%s".formatted(sceneCode, index, extension)).normalize();
            if (!Files.exists(target)) {
                return target;
            }
        }
        throw new IllegalStateException("no available meme image filename in scene directory");
    }

    private String normalizeSceneCode(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("sceneCode is required");
        }
        String sceneCode = value.strip();
        if (!sceneCode.matches("[a-z0-9_\\-]+")) {
            throw new IllegalArgumentException("sceneCode must use lowercase letters, numbers, '_' or '-'");
        }
        return sceneCode;
    }

    private String extension(String filename) {
        if (!hasText(filename)) {
            throw new IllegalArgumentException("meme image filename is required");
        }
        String normalized = filename.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("meme image file extension is required");
        }
        String extension = normalized.substring(dotIndex + 1);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("meme image file extension must be png, jpg, jpeg, gif or webp");
        }
        return extension;
    }

    private Path memeBaseDir() {
        String baseDir = properties.getMeme() == null ? null : properties.getMeme().getBaseDir();
        if (!hasText(baseDir)) {
            baseDir = DEFAULT_BASE_DIR;
        }
        Path path = Path.of(baseDir.strip());
        return path.isAbsolute() ? path.normalize() : path.toAbsolutePath().normalize();
    }

    private void ensureInside(Path baseDir, Path target, String message) {
        if (!target.normalize().startsWith(baseDir.normalize())) {
            throw new IllegalArgumentException(message);
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record UploadResult(
            String relativePath,
            String fileName,
            String resolvedPath,
            String oneBotFile,
            long size,
            String contentType) {
    }
}
