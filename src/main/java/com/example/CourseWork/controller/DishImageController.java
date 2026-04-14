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

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileUrl = saveDishDerivatives(file.getBytes(), uploadPath);
            log.info(">>> DISH IMAGE: Uploaded {} to {}", originalFilename, fileUrl);

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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileUrl = saveDishDerivatives(file.getBytes(), uploadPath);
            log.info(">>> DISH IMAGE: Associated {} ({}) with dish {}", fileUrl, originalFilename, id);

            dishService.updateDishImage(id, fileUrl);

            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (Exception e) {
            log.error(">>> DISH IMAGE: Upload failed for dish {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не вдалося зберегти файл: " + e.getMessage()));
        }
    }

    /**
     * Writes {@code {uuid}.jpg} (max 1024), {@code {uuid}_960.jpg}, {@code {uuid}_640.jpg}; returns public path to master.
     */
    private static String saveDishDerivatives(byte[] bytes, Path uploadPath) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String masterName = uuid + ".jpg";
        Path master = uploadPath.resolve(masterName);

        Thumbnails.of(new ByteArrayInputStream(bytes))
                .size(1024, 1024)
                .outputFormat("jpg")
                .outputQuality(0.82)
                .toFile(master.toFile());

        Thumbnails.of(new ByteArrayInputStream(bytes))
                .size(960, 960)
                .outputFormat("jpg")
                .outputQuality(0.80)
                .toFile(uploadPath.resolve(uuid + "_960.jpg").toFile());

        Thumbnails.of(new ByteArrayInputStream(bytes))
                .size(640, 640)
                .outputFormat("jpg")
                .outputQuality(0.78)
                .toFile(uploadPath.resolve(uuid + "_640.jpg").toFile());

        return "/uploads/" + masterName;
    }
}
