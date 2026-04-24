package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuDto;
import com.example.CourseWork.dto.menu.MenuSummaryDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;

import java.util.List;

public interface MenuService {
    List<MenuWithDishesDto> getAllMenusWithDishes();
    List<MenuSummaryDto> getActiveMenusSummary();
    List<MenuWithDishesDto> getActiveMenusWithDishes(String filter);
    MenuResponseDto createMenu(MenuDto dto);
    MenuResponseDto updateMenu(Integer id, MenuDto dto);
    void deleteMenu(Integer id);
}
