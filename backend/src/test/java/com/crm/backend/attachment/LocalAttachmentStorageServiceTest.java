package com.crm.backend.attachment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAttachmentStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    private LocalAttachmentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalAttachmentStorageService(
                temporaryDirectory.toString(),
                1024,
                "text/plain,application/pdf"
        );

        storageService.initializeStorage();
    }

    @Test
    void shouldStoreAndLoadAllowedFile() throws Exception {
        byte[] content = "Tadamun attachment test".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                content
        );

        StoredFile stored = storageService.store(file);
        Resource resource = storageService.load(stored.storageKey());

        assertTrue(resource.exists());
        assertEquals(content.length, stored.sizeBytes());
        assertEquals("text/plain", stored.contentType());
        assertEquals(64, stored.checksumSha256().length());

        try (var input = resource.getInputStream()) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void shouldRejectDisallowedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "page.html",
                "text/html",
                "<html></html>".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(file)
        );
    }

    @Test
    void shouldRejectOversizedFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[2048]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(file)
        );
    }

    @Test
    void shouldRejectInvalidStorageKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.load("../../secret.txt")
        );
    }
}