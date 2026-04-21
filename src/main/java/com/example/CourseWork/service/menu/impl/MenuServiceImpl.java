package com.example.CourseWork.service.menu.impl;

import com.example.CourseWork.dto.menu.MenuDto;
import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuWithDishesDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.MenuMapper;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.menu.MenuService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalTime;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

import com.example.CourseWork.service.menu.DishRatingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final DishRepository dishRepository;
    private final MenuMapper menuMapper;
    private final DishRatingService dishRatingService;
    private final Clock appClock;

    @Transactional
    @Override
    @Cacheable(cacheNames = "menusWithDishes")
    public List<MenuWithDishesDto> getAllMenusWithDishes() {
        List<Menu> menus = menuRepository.findAllWithDishes();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        // Compare menu time windows in configured app time zone; ignore seconds/nanos.
        LocalTime now = LocalTime.now(appClock).truncatedTo(ChronoUnit.MINUTES);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));

        List<MenuWithDishesDto> dtos = menus.stream()
                .sorted((m1, m2) -> {
                    boolean m1HasTime = m1.getStartTime() != null || m1.getEndTime() != null;
                    boolean m2HasTime = m2.getStartTime() != null || m2.getEndTime() != null;
                    if (m1HasTime && !m2HasTime)
                        return -1;
                    if (!m1HasTime && m2HasTime)
                        return 1;
                    return m1.getId().compareTo(m2.getId());
                })
                .filter(menu -> isStaff || isMenuActiveNow(menu, now))
                .map(menuMapper::toMenuWithDishesDto)
                .collect(Collectors.toCollection(ArrayList::new));

        attachDishTags(dtos);

        // Note: We intentionally do NOT inject synthetic recommendation menus into the
        // main menu list.
        // Recommendations/popular dishes are shown as dedicated sections on the home
        // page instead.

        // Enrich all dishes in all menus with aggregated ratings (avg + count)
        java.util.List<com.example.CourseWork.dto.menu.DishResponseDto> allDishes = dtos.stream()
                .filter(m -> m.getDishes() != null)
                .flatMap(m -> m.getDishes().stream())
                .collect(java.util.stream.Collectors.toList());
        dishRatingService.enrichWithRatings(allDishes);

        return dtos;
    }

    /**
     * Load tags for all dishes in one query (avoids N+1 on dish_tags after tags became LAZY).
     */
    private void attachDishTags(List<MenuWithDishesDto> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        List<Integer> dishIds = menus.stream()
                .filter(m -> m.getDishes() != null)
                .flatMap(m -> m.getDishes().stream())
                .map(com.example.CourseWork.dto.menu.DishResponseDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return;
        }

        Map<Integer, List<String>> tagsByDishId = new HashMap<>();
        for (Dish d : dishRepository.findAllByIdWithTags(dishIds)) {
            if (d == null || d.getId() == null) {
                continue;
            }
            List<String> tags = d.getTags();
            tagsByDishId.put(d.getId(), tags == null ? List.of() : new ArrayList<>(tags));
        }

        for (MenuWithDishesDto menu : menus) {
            if (menu.getDishes() == null) {
                continue;
            }
            for (com.example.CourseWork.dto.menu.DishResponseDto dishDto : menu.getDishes()) {
                if (dishDto == null || dishDto.getId() == null) {
                    continue;
                }
                dishDto.setTags(tagsByDishId.getOrDefault(dishDto.getId(), List.of()));
            }
        }
    }

    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
    public MenuResponseDto createMenu(MenuDto dto) {
        Menu menu = new Menu();
        menu.setName(dto.getName());
        menu.setStartTime(dto.getStartTime());
        menu.setEndTime(dto.getEndTime());
        return menuMapper.toResponseDto(menuRepository.save(menu));
    }

    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
    public MenuResponseDto updateMenu(Integer id, MenuDto dto) {
        @SuppressWarnings("null")
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.MENU_NOT_FOUND));
        menu.setName(dto.getName());
        menu.setStartTime(dto.getStartTime());
        menu.setEndTime(dto.getEndTime());
        Menu updated = menuRepository.save(menu);
        return menuMapper.toResponseDto(updated);
    }

    @Override
    @CacheEvict(cacheNames = "menusWithDishes", allEntries = true)
    @SuppressWarnings("null")
    public void deleteMenu(Integer id) {
        @SuppressWarnings("null")
        boolean exists = menuRepository.existsById(id);
        if (!exists) {
            throw new NotFoundException(ErrorMessages.MENU_NOT_FOUND);
        }
        menuRepository.deleteById(id);
    }

    private boolean isMenuActiveNow(Menu menu, LocalTime now) {
        if (menu.getStartTime() == null || menu.getEndTime() == null) {
            return true;
        }
        if (menu.getStartTime().isBefore(menu.getEndTime())) {
            return !now.isBefore(menu.getStartTime()) && !now.isAfter(menu.getEndTime());
        } else {
            // crosses midnight
            return !now.isBefore(menu.getStartTime()) || !now.isAfter(menu.getEndTime());
        }
    }
}