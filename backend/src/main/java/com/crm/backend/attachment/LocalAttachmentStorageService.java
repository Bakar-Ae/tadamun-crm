package com.crm.backend.attachment;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocalAttachmentStorageService {

    private static final String STORAGE_KEY_PATTERN =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    private final Path storageRoot;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public LocalAttachmentStorageService(
            @Value("${app.attachments.storage-path}") String storagePath,
            @Value("${app.attachments.max-size-bytes}") long maxSizeBytes,
            @Value("${app.attachments.allowed-content-types}")
            String allowedContentTypes
    ) {
        this.storageRoot = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();

        this.maxSizeBytes = maxSizeBytes;

        this.allowedContentTypes = Arrays.stream(
                        allowedContentTypes.split(",")
                )
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize attachment storage",
                    exception
            );
        }
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType()
                .trim()
                .toLowerCase(Locale.ROOT);

        String storageKey = UUID.randomUUID().toString();
        Path target = resolveStoragePath(storageKey);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long totalBytes = copyFile(file, target, digest);

            return new StoredFile(
                    storageKey,
                    contentType,
                    totalBytes,
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            deletePartialFile(target);

            throw new IllegalStateException(
                    "Could not store attachment",
                    exception
            );
        } catch (RuntimeException exception) {
            deletePartialFile(target);
            throw exception;
        }
    }

    public Resource load(String storageKey) {
        Path path = resolveStoragePath(storageKey);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Attachment file not found");
        }

        return new FileSystemResource(path);
    }

    public void delete(String storageKey) {
        Path path = resolveStoragePath(storageKey);

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not delete attachment file",
                    exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file is required");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "Attachment exceeds the maximum allowed size"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !allowedContentTypes.contains(
                contentType.trim().toLowerCase(Locale.ROOT)
        )) {
            throw new IllegalArgumentException(
                    "Attachment file type is not allowed"
            );
        }
    }

    private long copyFile(
            MultipartFile file,
            Path target,
            MessageDigest digest
    ) throws IOException {
        long totalBytes = 0;
        byte[] buffer = new byte[8192];

        try (
                InputStream input = new DigestInputStream(
                        file.getInputStream(),
                        digest
                );
                OutputStream output = Files.newOutputStream(
                        target,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )
        ) {
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                totalBytes += bytesRead;

                if (totalBytes > maxSizeBytes) {
                    throw new IllegalArgumentException(
                            "Attachment exceeds the maximum allowed size"
                    );
                }

                output.write(buffer, 0, bytesRead);
            }
        }

        return totalBytes;
    }

    private Path resolveStoragePath(String storageKey) {
        if (storageKey == null
                || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            throw new IllegalArgumentException("Invalid attachment storage key");
        }

        Path resolved = storageRoot.resolve(storageKey).normalize();

        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid attachment storage path");
        }

        return resolved;
    }

    private void deletePartialFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Preserve the original storage error.
        }
    }
}