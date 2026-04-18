package com.example.CourseWork.service.image.impl;

import com.example.CourseWork.service.image.ImageService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {

    // Keep production-friendly default (/app/uploads), but allow overriding via app.upload.dir.
    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Override
    public String processAndSaveImage(MultipartFile file) throws IOException {
        Path uploadPath = resolveUploadPath();
        uploadPath = ensureUploadPath(uploadPath);

        String baseName = UUID.randomUUID().toString();
        List<Integer> widths = List.of(320, 480, 640, 800);

        // We read it once into memory to avoid multiple stream reads or temp files
        byte[] imageBytes = file.getBytes();

        for (int w : widths) {
            Path targetPath = uploadPath.resolve(baseName + "-w" + w + ".webp");
            try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes)) {
                Thumbnails.of(bais)
                        .size(w, w)
                        .keepAspectRatio(true)
                        .outputFormat("webp")
                        .outputQuality(w <= 480 ? 0.78 : 0.80)
                        .toFile(targetPath.toFile());
            } catch (Throwable t) {
                // Catch Throwable to handle UnsatisfiedLinkError for WebP on some systems (like Mac aarch64)
                log.error("Failed to process image as WebP for width {}. Error: {}", w, t.getMessage());
                throw new IOException("Image processing failed", t);
            }
        }

        // Backwards compatibility: keep the old URLs working.
        // - main: /uploads/<uuid>.webp (points to 800w)
        // - thumb: /uploads/<uuid>-thumb.webp (points to 320w)
        Files.copy(uploadPath.resolve(baseName + "-w800.webp"), uploadPath.resolve(baseName + ".webp"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(uploadPath.resolve(baseName + "-w320.webp"), uploadPath.resolve(baseName + "-thumb.webp"), StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + baseName + ".webp";
    }

    private Path resolveUploadPath() {
        Path p = Paths.get(uploadDir);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
        }
        return p;
    }

    private static Path ensureUploadPath(Path uploadPath) throws IOException {
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            // Smoke-check writability (createDirectories doesn't guarantee write perms).
            Path probe = uploadPath.resolve(".write_test");
            Files.deleteIfExists(probe);
            Files.createFile(probe);
            Files.deleteIfExists(probe);
            return uploadPath;
        } catch (IOException e) {
            // Fallback for dev environments without permission to write into /app/uploads.
            Path fallback = Paths.get(System.getProperty("user.dir")).resolve("uploads").normalize();
            log.warn("Upload dir '{}' not writable; falling back to '{}'", uploadPath, fallback);
            if (!Files.exists(fallback)) {
                Files.createDirectories(fallback);
            }
            Path probe = fallback.resolve(".write_test");
            Files.deleteIfExists(probe);
            Files.createFile(probe);
            Files.deleteIfExists(probe);
            return fallback;
        }
    }
}
