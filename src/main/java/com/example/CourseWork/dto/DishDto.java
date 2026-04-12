package com.example.CourseWork.dto;

import lombok.Data;
import java.util.List;

@Data
public class DishDto {
    private String name;
    private String description;
    private Float price;
    private String imageUrl;
    private Boolean isAvailable;
    private List<Integer> menuIds;
}
