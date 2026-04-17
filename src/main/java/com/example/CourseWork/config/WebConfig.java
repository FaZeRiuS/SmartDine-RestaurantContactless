package com.example.CourseWork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

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
                .addResourceLocations("file:" + normalizeUploadDir(uploadDir) + "/")
                // Uploaded files are stored under UUID-based names, so they are safe to cache aggressively.
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        // Cache static assets aggressively; the app already versions them via query params (e.g. v=1.4).
        CacheControl staticCache = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(staticCache);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(staticCache);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(staticCache);
        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/")
                .setCacheControl(staticCache);
        registry.addResourceHandler("/manifest.json", "/sw.js", "/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(staticCache);
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