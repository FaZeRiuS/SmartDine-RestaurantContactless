package com.example.CourseWork.mapper;

import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.model.Menu;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class MenuMapper {
    
    private final DishMapper dishMapper;

    public MenuMapper(DishMapper dishMapper) {
        this.dishMapper = dishMapper;
    }
    
    public MenuResponseDto toResponseDto(Menu menu) {
        MenuResponseDto dto = new MenuResponseDto();
        dto.setId(menu.getId());
        dto.setName(menu.getName());
        dto.setStartTime(menu.getStartTime());
        dto.setEndTime(menu.getEndTime());
        return dto;
    }

    public MenuWithDishesDto toMenuWithDishesDto(Menu menu) {
        MenuWithDishesDto dto = new MenuWithDishesDto();
        dto.setId(menu.getId());
        dto.setName(menu.getName());
        dto.setStartTime(menu.getStartTime());
        dto.setEndTime(menu.getEndTime());
        dto.setDishes(menu.getDishes().stream()
                .map(dishMapper::toResponseDto)
                .collect(Collectors.toList()));
        return dto;
    }
}