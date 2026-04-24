package com.example.CourseWork.dto.menu;

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
    private Boolean isAvailable;
    private Double avgRating;
    private Long ratingsCount;
    private List<Integer> menuIds;
    private List<String> tags;
    private Integer preparationTime;
}

