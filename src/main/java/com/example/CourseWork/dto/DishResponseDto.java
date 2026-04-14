package com.example.CourseWork.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DishResponseDto {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    /** Responsive variants for {@code /uploads/*.jpg} (optional; set by mapper). */
    private String imageUrlSmall;
    private String imageUrlMedium;
    private Boolean isAvailable;
    private Double avgRating;
    private Long ratingsCount;
    private List<Integer> menuIds;
    private List<String> tags;
}

