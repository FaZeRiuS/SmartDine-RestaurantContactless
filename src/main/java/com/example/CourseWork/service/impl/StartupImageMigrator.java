package com.example.CourseWork.service.impl;

import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Automated migration service that runs on startup to optimize existing dish images.
 * It converts JPG/PNG images to WebP, generates thumbnails, and backups originals.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupImageMigrator implements CommandLineRunner {

    private final DishRepository dishRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Override
    public void run(String... args) {
        log.info(">>> PERFORMANCE: Starting automated image migration to WebP...");
        try {
            transactionTemplate.execute(status -> {
                processMigration();
                return null;
            });
        } catch (Exception e) {
            log.error(">>> PERFORMANCE: Image migration failed!", e);
        }
    }

    private void processMigration() {
        List<Dish> dishes = dishRepository.findAll();
        Path uploadPath = Paths.get(uploadDir);
        Path backupPath = uploadPath.resolve("backup");

        if (!Files.exists(uploadPath)) {
            log.warn(">>> PERFORMANCE: Upload directory does not exist: {}", uploadDir);
            return;
        }

        if (!Files.exists(backupPath)) {
            try {
                Files.createDirectories(backupPath);
            } catch (IOException e) {
                log.error(">>> PERFORMANCE: Could not create backup directory", e);
                return;
            }
        }

        int migratedCount = 0;
        for (Dish dish : dishes) {
            String imageUrl = dish.getImageUrl();
            if (imageUrl != null && isLegacyFormat(imageUrl)) {
                if (migrateDishImage(dish, uploadPath, backupPath)) {
                    migratedCount++;
                }
            }
        }
        
        if (migratedCount > 0) {
            log.info(">>> PERFORMANCE: Migration finished. {} images successfully converted to WebP.", migratedCount);
        } else {
            log.info(">>> PERFORMANCE: No legacy images found for migration.");
        }
    }

    private boolean isLegacyFormat(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }

    private boolean migrateDishImage(Dish dish, Path uploadPath, Path backupPath) {
        String imageUrl = dish.getImageUrl();
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        Path sourcePath = uploadPath.resolve(filename);

        if (!Files.exists(sourcePath)) {
            log.warn(">>> PERFORMANCE: Source file not found on disk: {}", sourcePath);
            return false;
        }

        String baseName = FilenameUtils.getBaseName(filename);
        String webpFilename = baseName + ".webp";
        String thumbFilename = baseName + "-thumb.webp";
        Path targetPath = uploadPath.resolve(webpFilename);
        Path thumbPath = uploadPath.resolve(thumbFilename);

        try {
            // 1. Convert to Main WebP (800px)
            Thumbnails.of(sourcePath.toFile())
                    .size(800, 800)
                    .keepAspectRatio(true)
                    .outputFormat("webp")
                    .outputQuality(0.82)
                    .toFile(targetPath.toFile());

            // 2. Generate WebP Thumbnail (400px)
            Thumbnails.of(sourcePath.toFile())
                    .size(400, 400)
                    .keepAspectRatio(true)
                    .outputFormat("webp")
                    .outputQuality(0.75)
                    .toFile(thumbPath.toFile());

            // 3. Move original to backup folder (User requirement: keep originals)
            FileUtils.moveFileToDirectory(sourcePath.toFile(), backupPath.toFile(), true);

            // 4. Update Database record to point to new WebP
            dish.setImageUrl("/uploads/" + webpFilename);
            dishRepository.save(dish);

            log.info(">>> PERFORMANCE: Migrated {} -> {}", filename, webpFilename);
            return true;
        } catch (IOException e) {
            log.error(">>> PERFORMANCE: Failed to migrate image {}", filename, e);
            return false;
        }
    }
}
