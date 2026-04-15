package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishDto;
import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.DishRatingService;
import com.example.CourseWork.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final MenuRepository menuRepository;
    private final DishMapper dishMapper;
    private final DishRatingService dishRatingService;

    @Override
    public DishResponseDto createDish(DishDto dto) {
        List<Integer> menuIds = dto.getMenuIds() == null ? Collections.emptyList() : dto.getMenuIds();
        List<Menu> menus = menuIds.isEmpty() ? Collections.emptyList() : menuRepository.findAllById(menuIds);
        if (menus.isEmpty() && !menuIds.isEmpty()) {
            throw new NotFoundException(ErrorMessages.MENUS_NOT_FOUND);
        }

        Dish dish = new Dish();
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setIsAvailable(dto.getIsAvailable());
        dish.setImageUrl(dto.getImageUrl());
        dish.setMenus(menus);
        dish.setTags(dto.getTags() == null ? new ArrayList<>() : new ArrayList<>(dto.getTags()));

        return dishMapper.toResponseDto(dishRepository.save(dish));
    }

    @Override
    public DishResponseDto updateDish(Integer id, DishDto dto) {
        @SuppressWarnings("null")
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));

        List<Integer> menuIds = dto.getMenuIds() == null ? Collections.emptyList() : dto.getMenuIds();
        List<Menu> menus = menuIds.isEmpty() ? Collections.emptyList() : menuRepository.findAllById(menuIds);
        if (menus.isEmpty() && !menuIds.isEmpty()) {
            throw new NotFoundException(ErrorMessages.MENUS_NOT_FOUND);
        }

        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setIsAvailable(dto.getIsAvailable());
        dish.setImageUrl(dto.getImageUrl());
        dish.setMenus(menus);
        dish.setTags(dto.getTags() == null ? new ArrayList<>() : new ArrayList<>(dto.getTags()));

        return dishMapper.toResponseDto(dishRepository.save(dish));
    }

    @Override
    @SuppressWarnings("null")
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
    @SuppressWarnings("null")
    public DishResponseDto getDishById(Integer id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));
        DishResponseDto dto = dishMapper.toResponseDto(dish);
        dishRatingService.enrichWithRatings(java.util.List.of(dto));
        return dto;
    }

    @Override
    public Optional<DishResponseDto> getSmartCombo(Integer dishId, List<Integer> existingIds) {
        Dish dish = dishRepository.findSmartComboForDish(dishId)
                .orElseGet(() -> dishRepository.findPopularComboFallback(dishId).orElse(null));
                
        if (dish != null && existingIds != null && !existingIds.isEmpty()) {
            boolean sharesMenu = dishRepository.checkIfSharesMenu(dish.getId(), existingIds);
            if (sharesMenu) {
                return Optional.empty();
            }
        }
        
        if (dish == null) return Optional.empty();
        DishResponseDto dto = dishMapper.toResponseDto(dish);
        dishRatingService.enrichWithRatings(java.util.List.of(dto));
        return Optional.of(dto);
    }
    @Override
    public void updateDishImage(Integer id, String imageUrl) {
        @SuppressWarnings("null")
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));
        dish.setImageUrl(imageUrl);
        dishRepository.save(dish);
    }
}
