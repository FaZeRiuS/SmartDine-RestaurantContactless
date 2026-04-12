package com.example.CourseWork.dto;

import lombok.Data;
import java.util.List;

@Data
public class DishResponseDto {
    private Integer id;
    private String name;
    private String description;
    private Float price;
    private String imageUrl;
    private Boolean isAvailable;
    private Double avgRating;
    private Long ratingsCount;
    private List<Integer> menuIds;
    private List<String> tags;
}

