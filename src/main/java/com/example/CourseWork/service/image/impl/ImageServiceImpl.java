package com.example.CourseWork.service.image.impl;

import com.example.CourseWork.service.image.ImageService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * WebP is fast and small, but can be unstable on some deployments depending on ImageIO plugins/native libs.
     * Allow disabling on prod via env: APP_IMAGES_WEBP_ENABLED=false
     */
    @Value("${app.images.webp.enabled:true}")
    private boolean webpEnabled;

    @Override
    public String processAndSaveImage(MultipartFile file) throws IOException {
        Path uploadPath = resolveUploadPath();
        uploadPath = ensureUploadPath(uploadPath);

        String baseName = UUID.randomUUID().toString();
        BufferedImage img = readAndValidateImage(file);

        // Prefer WebP variants when enabled and writer exists; otherwise save original as-is.
        if (webpEnabled && hasWebpWriter()) {
            try {
                writeWebpVariants(uploadPath, baseName, img);
                // Backwards compatibility: keep the old URLs working.
                // - main: /uploads/<uuid>.webp (points to 800w)
                // - thumb: /uploads/<uuid>-thumb.webp (points to 320w)
                Files.copy(uploadPath.resolve(baseName + "-w800.webp"), uploadPath.resolve(baseName + ".webp"),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.copy(uploadPath.resolve(baseName + "-w320.webp"), uploadPath.resolve(baseName + "-thumb.webp"),
                        StandardCopyOption.REPLACE_EXISTING);
                return "/uploads/" + baseName + ".webp";
            } catch (Exception e) {
                // Important: never let image processing take down the whole service.
                log.error("WebP processing failed; falling back to original upload. {}", e.getMessage());
            }
        }

        return saveOriginal(uploadPath, baseName, file);
    }

    private static BufferedImage readAndValidateImage(MultipartFile file) throws IOException {
        // Decode once and validate dimensions to avoid "image bombs" (huge decompressed bitmaps) and OOM/restarts.
        try (InputStream in = file.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IOException("Unsupported image format");
            }
            int w = img.getWidth();
            int h = img.getHeight();
            long pixels = (long) w * (long) h;
            // 20 megapixels cap: plenty for dish images, protects memory/cpu.
            if (w <= 0 || h <= 0 || pixels > 20_000_000L) {
                throw new IOException("Image too large");
            }
            return img;
        }
    }

    private static boolean hasWebpWriter() {
        try {
            return ImageIO.getImageWritersByFormatName("webp").hasNext();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void writeWebpVariants(Path uploadPath, String baseName, BufferedImage img) throws IOException {
        List<Integer> widths = List.of(320, 480, 640, 800);
        for (int w : widths) {
            Path targetPath = uploadPath.resolve(baseName + "-w" + w + ".webp");
            Thumbnails.of(img)
                    .size(w, w)
                    .keepAspectRatio(true)
                    .outputFormat("webp")
                    .outputQuality(w <= 480 ? 0.78 : 0.80)
                    .toFile(targetPath.toFile());
        }
    }

    private static String saveOriginal(Path uploadPath, String baseName, MultipartFile file) throws IOException {
        String ext = guessSafeExtension(file);
        String filename = baseName + ext;
        Path target = uploadPath.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + filename;
    }

    private static String guessSafeExtension(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null) {
            String c = ct.toLowerCase();
            if (c.contains("png")) return ".png";
            if (c.contains("jpeg") || c.contains("jpg")) return ".jpg";
            if (c.contains("webp")) return ".webp";
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".png")) return ".png";
            if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) return ".jpg";
            if (lower.endsWith(".webp")) return ".webp";
        }
        return ".jpg";
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
