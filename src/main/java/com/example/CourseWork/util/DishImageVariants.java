package com.example.CourseWork.util;

import com.example.CourseWork.dto.DishResponseDto;

/**
 * Local uploads use {@code /uploads/{uuid}.jpg} plus {@code _640} / {@code _960} derivatives.
 */
public final class DishImageVariants {

    private DishImageVariants() {
    }

    public static void apply(DishResponseDto dto) {
        if (dto == null) {
            return;
        }
        String url = dto.getImageUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        if (!url.startsWith("/uploads/") || !url.endsWith(".jpg")) {
            return;
        }
        if (url.contains("_640") || url.contains("_960")) {
            return;
        }
        String stem = url.substring(0, url.length() - ".jpg".length());
        dto.setImageUrlSmall(stem + "_640.jpg");
        dto.setImageUrlMedium(stem + "_960.jpg");
    }
}
