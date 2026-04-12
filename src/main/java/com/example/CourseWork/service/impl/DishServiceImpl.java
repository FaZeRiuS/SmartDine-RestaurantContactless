package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishDto;
import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.DishRatingService;
import com.example.CourseWork.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final MenuRepository menuRepository;
    private final DishMapper dishMapper;
    private final DishRatingService dishRatingService;

    @Override
    public DishResponseDto createDish(DishDto dto) {
        List<Menu> menus = menuRepository.findAllById(dto.getMenuIds());
        if (menus.isEmpty() && dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            throw new RuntimeException("Menus not found");
        }

        Dish dish = new Dish();
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setIsAvailable(dto.getIsAvailable());
        dish.setImageUrl(dto.getImageUrl());
        dish.setMenus(menus);

        return dishMapper.toResponseDto(dishRepository.save(dish));
    }

    @Override
    public DishResponseDto updateDish(Integer id, DishDto dto) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish not found"));

        List<Menu> menus = menuRepository.findAllById(dto.getMenuIds());
        if (menus.isEmpty() && dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            throw new RuntimeException("Menus not found");
        }

        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setIsAvailable(dto.getIsAvailable());
        dish.setImageUrl(dto.getImageUrl());
        dish.setMenus(menus);

        return dishMapper.toResponseDto(dishRepository.save(dish));
    }

    @Override
    public void deleteDish(Integer id) {
        dishRepository.deleteById(id);
    }

    @Override
    public List<DishResponseDto> getAllAvailableDishes() {
        List<DishResponseDto> dishes = dishRepository.findByIsAvailableTrue()
                .stream()
                .map(dishMapper::toResponseDto)
                .collect(Collectors.toList());
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    public List<DishResponseDto> getAllDishes() {
        List<DishResponseDto> dishes = dishRepository.findAll()
                .stream()
                .map(dishMapper::toResponseDto)
                .collect(Collectors.toList());
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    public DishResponseDto getDishById(Integer id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish not found"));
        DishResponseDto dto = dishMapper.toResponseDto(dish);
        dishRatingService.enrichWithRatings(java.util.List.of(dto));
        return dto;
    }

    @Override
    public DishResponseDto getSmartCombo(Integer dishId, List<Integer> existingIds) {
        Dish dish = dishRepository.findSmartComboForDish(dishId)
                .orElseGet(() -> dishRepository.findPopularComboFallback(dishId).orElse(null));
                
        if (dish != null && existingIds != null && !existingIds.isEmpty()) {
            boolean sharesMenu = dishRepository.checkIfSharesMenu(dish.getId(), existingIds);
            if (sharesMenu) {
                return null;
            }
        }
        
        if (dish == null) return null;
        DishResponseDto dto = dishMapper.toResponseDto(dish);
        dishRatingService.enrichWithRatings(java.util.List.of(dto));
        return dto;
    }
    @Override
    public void updateDishImage(Integer id, String imageUrl) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish not found"));
        dish.setImageUrl(imageUrl);
        dishRepository.save(dish);
    }
}
