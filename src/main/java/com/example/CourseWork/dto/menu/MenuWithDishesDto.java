package com.example.CourseWork.dto.menu;

import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class MenuWithDishesDto {
    private Integer id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<DishResponseDto> dishes;
}
