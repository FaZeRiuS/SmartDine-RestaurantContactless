package com.example.CourseWork.service;

import com.example.CourseWork.dto.MenuResponseDto;
import com.example.CourseWork.dto.MenuDto;
import com.example.CourseWork.dto.MenuWithDishesDto;

import java.util.List;

public interface MenuService {
    List<MenuWithDishesDto> getAllMenusWithDishes();
    MenuResponseDto createMenu(MenuDto dto);
    MenuResponseDto updateMenu(Integer id, MenuDto dto);
    void deleteMenu(Integer id);
}
