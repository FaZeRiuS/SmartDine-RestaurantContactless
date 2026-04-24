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
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
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
        dish.setAllergens(dto.getAllergens() == null ? new ArrayList<>() : new ArrayList<>(dto.getAllergens()));

        DishResponseDto out = dishMapper.toResponseDto(dishRepository.save(dish));
        out.setMenuIds(menuIds);
        out.setTags(dto.getTags() == null ? java.util.List.of() : dto.getTags());
        out.setAllergens(dto.getAllergens() == null ? java.util.List.of() : dto.getAllergens());
        return out;
    }

    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
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
        dish.setAllergens(dto.getAllergens() == null ? new ArrayList<>() : new ArrayList<>(dto.getAllergens()));

        DishResponseDto out = dishMapper.toResponseDto(dishRepository.save(dish));
        out.setMenuIds(menuIds);
        out.setTags(dto.getTags() == null ? java.util.List.of() : dto.getTags());
        out.setAllergens(dto.getAllergens() == null ? java.util.List.of() : dto.getAllergens());
        return out;
    }

    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
    @SuppressWarnings("null")
    public void deleteDish(Integer id) {
        dishRepository.deleteById(id);
    }

    @Override
    public List<DishResponseDto> getAllAvailableDishes() {
        List<DishResponseDto> dishes = dishRepository.findAvailableWithTags().stream()
                .map(d -> {
                    DishResponseDto dto = dishMapper.toResponseDto(d);
                    dto.setTags(d.getTags());
                    return dto;
                })
                .collect(Collectors.toList());
        attachMenuIds(dishes);
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    public List<DishResponseDto> getPopularDishesForHome(int maxDishes, int maxPerMenu, Set<Integer> excludeDishIds) {
        return getPopularDishesForHome(maxDishes, maxPerMenu, excludeDishIds, Set.of());
    }

    @Override
    public List<DishResponseDto> getPopularDishesForHome(int maxDishes, int maxPerMenu, Set<Integer> excludeDishIds, Set<String> excludeAllergens) {
        Set<Integer> exclude = excludeDishIds == null ? Set.of() : excludeDishIds;
        Set<String> excludeAllergenSet = excludeAllergens == null ? Set.of() : excludeAllergens;
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

        Map<Integer, Dish> byIdWithTags = dishRepository.findAllByIdWithTags(chosenIds).stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));
        Map<Integer, Dish> byIdWithAllergens = dishRepository.findAllByIdWithAllergens(chosenIds).stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        List<DishResponseDto> result = new ArrayList<>();
        for (int id : chosenIds) {
            Dish dTags = byIdWithTags.get(id);
            Dish dAll = byIdWithAllergens.get(id);
            if (dTags != null) {
                List<String> allergens = dAll != null ? dAll.getAllergens() : List.of();
                if (!excludeAllergenSet.isEmpty() && allergens != null && allergens.stream().anyMatch(excludeAllergenSet::contains)) {
                    continue;
                }
                DishResponseDto dto = dishMapper.toResponseDto(dTags);
                dto.setTags(dTags.getTags());
                dto.setAllergens(allergens == null ? List.of() : allergens);
                result.add(dto);
            }
        }
        attachMenuIds(result);
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
        List<DishResponseDto> dishes = dishRepository.findAllWithTags().stream()
                .map(d -> {
                    DishResponseDto dto = dishMapper.toResponseDto(d);
                    dto.setTags(d.getTags());
                    return dto;
                })
                .collect(Collectors.toList());
        attachMenuIds(dishes);
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    @SuppressWarnings("null")
    public DishResponseDto getDishById(Integer id) {
        Dish dish = dishRepository.findByIdWithTags(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));
        DishResponseDto dto = dishMapper.toResponseDto(dish);
        dto.setTags(dish.getTags());
        attachMenuIds(java.util.List.of(dto));
        dishRatingService.enrichWithRatings(java.util.List.of(dto));
        return dto;
    }

    @Override
    public Optional<DishResponseDto> getSmartCombo(Integer dishId, List<Integer> existingIds) {
        return recommendationService.getCrossSellRecommendation(dishId, existingIds);
    }
    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
    public void updateDishImage(Integer id, String imageUrl) {
        @SuppressWarnings("null")
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));
        dish.setImageUrl(imageUrl);
        dishRepository.save(dish);
    }

    private void attachMenuIds(List<DishResponseDto> dishes) {
        if (dishes == null || dishes.isEmpty()) return;
        List<Integer> ids = dishes.stream()
                .map(DishResponseDto::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;

        List<Object[]> links = dishRepository.findDishMenuLinksForDishIds(ids);
        Map<Integer, List<Integer>> menuIdsByDish = new HashMap<>();
        for (Object[] link : links) {
            if (link == null || link.length < 2 || link[0] == null || link[1] == null) continue;
            int dishId = ((Number) link[0]).intValue();
            int menuId = ((Number) link[1]).intValue();
            menuIdsByDish.computeIfAbsent(dishId, k -> new ArrayList<>()).add(menuId);
        }
        dishes.forEach(d -> d.setMenuIds(menuIdsByDish.getOrDefault(d.getId(), List.of())));
    }

    @Override
    public List<String> getDistinctTags() {
        return dishRepository.findDistinctTags();
    }

    @Override
    public List<String> getDistinctAllergens() {
        return dishRepository.findDistinctAllergens();
    }
}
