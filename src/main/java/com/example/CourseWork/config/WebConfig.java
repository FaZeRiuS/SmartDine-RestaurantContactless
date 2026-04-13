package com.example.CourseWork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SecurityProperties securityProperties;
    private final String uploadDir;

    public WebConfig(SecurityProperties securityProperties,
                     @Value("${app.upload.dir:/app/uploads}") String uploadDir) {
        this.securityProperties = securityProperties;
        this.uploadDir = uploadDir;
    }

    @Override
    @SuppressWarnings("null")
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(securityProperties.getCorsAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizeUploadDir(uploadDir) + "/");
    }

    private static String normalizeUploadDir(String dir) {
        if (dir == null || dir.isBlank()) {
            return "/app/uploads";
        }
        // Ensure we don't end up with "file:/path//"
        if (dir.endsWith("/")) {
            return dir.substring(0, dir.length() - 1);
        }
        return dir;
    }
}