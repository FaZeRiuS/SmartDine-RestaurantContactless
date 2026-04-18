package com.example.CourseWork.service.menu.impl;

import com.example.CourseWork.dto.menu.DishDto;
import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.menu.DishRatingService;
import com.example.CourseWork.service.menu.DishService;
import com.example.CourseWork.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private static final int POPULAR_HOME_CANDIDATE_POOL = 120;

    private final DishRepository dishRepository;
    private final MenuRepository menuRepository;
    private final DishMapper dishMapper;
    private final DishRatingService dishRatingService;
    private final RecommendationService recommendationService;

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
    public List<DishResponseDto> getPopularDishesForHome(int maxDishes, int maxPerMenu, Set<Integer> excludeDishIds) {
        Set<Integer> exclude = excludeDishIds == null ? Set.of() : excludeDishIds;
        List<Object[]> popularityRows = dishRepository.findAvailableDishesOrderedByOrderVolume(POPULAR_HOME_CANDIDATE_POOL);
        if (popularityRows.isEmpty()) {
            return List.of();
        }

        List<Integer> candidateIds = new ArrayList<>();
        for (Object[] row : popularityRows) {
            if (row != null && row.length >= 1 && row[0] != null) {
                candidateIds.add(((Number) row[0]).intValue());
            }
        }
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<Object[]> menuLinks = dishRepository.findDishMenuLinksForDishIds(candidateIds);
        Map<Integer, List<Integer>> menusByDish = new HashMap<>();
        for (Object[] link : menuLinks) {
            if (link == null || link.length < 2 || link[0] == null || link[1] == null) {
                continue;
            }
            int dishId = ((Number) link[0]).intValue();
            int menuId = ((Number) link[1]).intValue();
            menusByDish.computeIfAbsent(dishId, k -> new ArrayList<>()).add(menuId);
        }

        Map<Integer, Integer> selectedCountByMenu = new HashMap<>();
        List<Integer> chosenIds = new ArrayList<>();

        for (int dishId : candidateIds) {
            if (exclude.contains(dishId)) {
                continue;
            }
            List<Integer> menus = menusByDish.getOrDefault(dishId, List.of());
            if (!canTakeDishForPopular(menus, selectedCountByMenu, maxPerMenu)) {
                continue;
            }
            for (int menuId : menus) {
                selectedCountByMenu.merge(menuId, 1, (a, b) -> a + b);
            }
            chosenIds.add(dishId);
            if (chosenIds.size() >= maxDishes) {
                break;
            }
        }

        if (chosenIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Dish> byId = dishRepository.findAllById(chosenIds).stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));
        List<DishResponseDto> result = new ArrayList<>();
        for (int id : chosenIds) {
            Dish d = byId.get(id);
            if (d != null) {
                result.add(dishMapper.toResponseDto(d));
            }
        }
        dishRatingService.enrichWithRatings(result);
        return result;
    }

    private static boolean canTakeDishForPopular(List<Integer> menuIds, Map<Integer, Integer> selectedCountByMenu, int maxPerMenu) {
        for (int menuId : menuIds) {
            if (selectedCountByMenu.getOrDefault(menuId, 0) >= maxPerMenu) {
                return false;
            }
        }
        return true;
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
        return recommendationService.getCrossSellRecommendation(dishId, existingIds);
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
