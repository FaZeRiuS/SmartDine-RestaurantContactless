package com.example.CourseWork.service.image;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ImageService {
    /**
     * Processes an uploaded image (resizing, WebP conversion) and saves it to storage.
     * @param file The uploaded multipart file.
     * @return The public URL of the saved image.
     * @throws IOException If image processing or saving fails.
     */
    String processAndSaveImage(MultipartFile file) throws IOException;
}
