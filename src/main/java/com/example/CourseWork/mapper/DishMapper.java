package com.example.CourseWork.mapper;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.model.Dish;
import org.springframework.stereotype.Component;

@Component
public class DishMapper {
    
    public DishResponseDto toResponseDto(Dish dish) {
        DishResponseDto dto = new DishResponseDto();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setPrice(dish.getPrice());
        dto.setIsAvailable(dish.getIsAvailable());
        dto.setImageUrl(dish.getImageUrl());
        dto.setMenuIds(dish.getMenus().stream().map(com.example.CourseWork.model.Menu::getId).collect(java.util.stream.Collectors.toList()));
        dto.setTags(dish.getTags());
        return dto;
    }
} 