package com.example.CourseWork.controller;

import com.example.CourseWork.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/dishes")
@RequiredArgsConstructor
public class DishImageController {

    private final DishService dishService;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMINISTRATOR')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл порожній"));
        }
        try {
            String fileUrl = processAndSaveImage(file);
            log.info(">>> DISH IMAGE: Uploaded {} -> {}", file.getOriginalFilename(), fileUrl);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (IOException e) {
            log.error(">>> DISH IMAGE: Upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не вдалося зберегти файл: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMINISTRATOR')")
    public ResponseEntity<?> uploadDishImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл порожній"));
        }
        try {
            String fileUrl = processAndSaveImage(file);
            log.info(">>> DISH IMAGE: Associated {} with dish {}", fileUrl, id);
            dishService.updateDishImage(id, fileUrl);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (Exception e) {
            log.error(">>> DISH IMAGE: Upload failed for dish {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не вдалося зберегти файл: " + e.getMessage()));
        }
    }

    /**
     * Compresses and saves an uploaded image as WebP (max 800×800px) and generates a thumbnail (max 400×400px).
     * WebP saves ~50% vs. JPEG at equivalent visual quality.
     *
     * @return public URL of the saved original image (e.g. "/uploads/uuid.webp")
     */
    private String processAndSaveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String baseName = UUID.randomUUID().toString();
        String mainFilename = baseName + ".webp";
        String thumbFilename = baseName + "-thumb.webp";

        Path mainPath = uploadPath.resolve(mainFilename);
        Path thumbPath = uploadPath.resolve(thumbFilename);

        // We read it once into memory to avoid multiple stream reads or temp files
        byte[] imageBytes = file.getBytes();

        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes)) {
            // 1. Generate Main Image (800px)
            Thumbnails.of(bais)
                    .size(800, 800)
                    .keepAspectRatio(true)
                    .outputFormat("webp")
                    .outputQuality(0.82)
                    .toFile(mainPath.toFile());
        }

        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes)) {
            // 2. Generate Thumbnail (400px)
            Thumbnails.of(bais)
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputFormat("webp")
                    .outputQuality(0.75) // Slightly lower quality for thumbs
                    .toFile(thumbPath.toFile());
        }

        return "/uploads/" + mainFilename;
    }
}
