package com.example.CourseWork.service.impl;

import com.example.CourseWork.service.ImageService;
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

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Override
    public String processAndSaveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

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
                        .outputQuality(w <= 480 ? 0.74 : 0.80)
                        .toFile(targetPath.toFile());
            } catch (Throwable t) {
                // Catch Throwable to handle UnsatisfiedLinkError for WebP on some systems (like Mac aarch64)
                log.error("Failed to process image as WebP for width {}. Error: {}", w, t.getMessage());
                throw new IOException("Image processing failed: " + t.getMessage(), t);
            }
        }

        // Backwards compatibility: keep the old URLs working.
        // - main: /uploads/<uuid>.webp (points to 800w)
        // - thumb: /uploads/<uuid>-thumb.webp (points to 320w)
        Files.copy(uploadPath.resolve(baseName + "-w800.webp"), uploadPath.resolve(baseName + ".webp"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(uploadPath.resolve(baseName + "-w320.webp"), uploadPath.resolve(baseName + "-thumb.webp"), StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + baseName + ".webp";
    }
}
