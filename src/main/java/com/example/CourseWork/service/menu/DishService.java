package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DishService {
    DishResponseDto createDish(DishDto dto);
    DishResponseDto updateDish(Integer id, DishDto dto);
    void deleteDish(Integer id);
    List<DishResponseDto> getAllAvailableDishes();

    /**
     * Home page "popular" block: top dishes by total ordered quantity, at most {@code maxPerMenu} dishes
     * counted per menu (a dish in several menus consumes one slot in each). Skips {@code excludeDishIds}.
     */
    List<DishResponseDto> getPopularDishesForHome(int maxDishes, int maxPerMenu, Set<Integer> excludeDishIds);

    List<DishResponseDto> getAllDishes();
    DishResponseDto getDishById(Integer id);
    Optional<DishResponseDto> getSmartCombo(Integer dishId, List<Integer> existingIds);
    void updateDishImage(Integer id, String imageUrl);
}
