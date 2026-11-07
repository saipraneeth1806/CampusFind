package com.example.campusfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(tempDir.toString());
    }

    @Test
    void nullOrEmptyFileReturnsNull() {
        assertNull(service.storeImage(null));
        MockMultipartFile emptyFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);
        assertNull(service.storeImage(emptyFile));
    }

    @Test
    void storesValidJpegImage() throws IOException {
        byte[] content = "fake jpeg content".getBytes();
        MockMultipartFile file = new MockMultipartFile("image", "sample.jpg", "image/jpeg", content);

        String filename = service.storeImage(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".jpg"));
        Path storedFile = tempDir.resolve(filename);
        assertTrue(Files.exists(storedFile));
        assertEquals(content.length, Files.size(storedFile));
    }

    @Test
    void storesValidPngImage() {
        byte[] content = "fake png content".getBytes();
        MockMultipartFile file = new MockMultipartFile("image", "sample.png", "image/png", content);

        String filename = service.storeImage(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
    }

    @Test
    void rejectsDisallowedMimeType() {
        MockMultipartFile file = new MockMultipartFile("image", "malicious.exe", "application/x-msdownload", "bad".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.storeImage(file));
        assertTrue(ex.getMessage().contains("Only JPG, PNG, WebP, or GIF"));
    }

    @Test
    void rejectsOversizedFile() {
        byte[] hugeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("image", "huge.jpg", "image/jpeg", hugeContent);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.storeImage(file));
        assertTrue(ex.getMessage().contains("5MB"));
    }

    @Test
    void deleteRemovesStoredFile() {
        MockMultipartFile file = new MockMultipartFile("image", "temp.jpg", "image/jpeg", "data".getBytes());
        String filename = service.storeImage(file);
        assertTrue(Files.exists(tempDir.resolve(filename)));

        boolean deleted = service.delete(filename);
        assertTrue(deleted);
        assertFalse(Files.exists(tempDir.resolve(filename)));
    }
}

