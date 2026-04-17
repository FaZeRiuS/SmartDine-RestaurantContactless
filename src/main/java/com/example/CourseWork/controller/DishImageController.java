package com.example.CourseWork.controller;

import com.example.CourseWork.service.DishService;
import com.example.CourseWork.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/dishes")
@RequiredArgsConstructor
public class DishImageController {

    private final DishService dishService;
    private final ImageService imageService;

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMINISTRATOR')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл порожній"));
        }
        try {
            String fileUrl = imageService.processAndSaveImage(file);
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
            String fileUrl = imageService.processAndSaveImage(file);
            log.info(">>> DISH IMAGE: Associated {} with dish {}", fileUrl, id);
            dishService.updateDishImage(id, fileUrl);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (Exception e) {
            log.error(">>> DISH IMAGE: Upload failed for dish {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Не вдалося зберегти файл: " + e.getMessage()));
        }
    }
}
