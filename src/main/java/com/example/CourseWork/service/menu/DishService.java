package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;

import java.util.List;
import java.util.Optional;

public interface DishService {
    DishResponseDto createDish(DishDto dto);
    DishResponseDto updateDish(Integer id, DishDto dto);
    void deleteDish(Integer id);
    List<DishResponseDto> getAllAvailableDishes();
    List<DishResponseDto> getAllDishes();
    DishResponseDto getDishById(Integer id);
    Optional<DishResponseDto> getSmartCombo(Integer dishId, List<Integer> existingIds);
    void updateDishImage(Integer id, String imageUrl);
}
