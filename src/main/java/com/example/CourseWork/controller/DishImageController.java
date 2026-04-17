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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
     * Compresses and saves an uploaded image as WebP (max 800×800px) and generates
     * a thumbnail (max 400×400px).
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
