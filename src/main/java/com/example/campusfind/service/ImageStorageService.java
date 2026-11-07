package com.example.campusfind.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path rootLocation;

    public ImageStorageService(@Value("${campusfind.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload storage location", e);
        }
    }

    /**
     * Validates and stores the uploaded image file.
     *
     * @param file the multipart file to store
     * @return the generated safe filename, or null if file is empty/null
     * @throws IllegalArgumentException if the file is invalid (too large or unsupported format)
     */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file size exceeds the 5MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPG, PNG, WebP, or GIF image formats are allowed.");
        }

        String extension = getExtensionForContentType(contentType.toLowerCase(Locale.ROOT));
        String filename = UUID.randomUUID().toString() + extension;
        Path destination = this.rootLocation.resolve(filename).normalize().toAbsolutePath();

        // Security check against directory traversal
        if (!destination.getParent().equals(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside target upload directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store image file.", e);
        }
    }

    public Path load(String filename) {
        return rootLocation.resolve(filename).normalize();
    }

    public boolean delete(String filename) {
        if (filename == null || filename.isBlank()) return false;
        try {
            Path file = load(filename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    private String getExtensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}

