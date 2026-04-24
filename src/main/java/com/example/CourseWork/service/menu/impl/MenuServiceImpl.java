package com.example.CourseWork.service.menu.impl;

import com.example.CourseWork.dto.menu.MenuDto;
import com.example.CourseWork.dto.menu.MenuResponseDto;
import com.example.CourseWork.dto.menu.MenuSummaryDto;
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
import java.util.Set;
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

    @Transactional
    @Override
    public List<MenuSummaryDto> getActiveMenusSummary() {
        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        LocalTime now = LocalTime.now(appClock).truncatedTo(ChronoUnit.MINUTES);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));

        return menus.stream()
                .sorted((m1, m2) -> {
                    boolean m1HasTime = m1.getStartTime() != null || m1.getEndTime() != null;
                    boolean m2HasTime = m2.getStartTime() != null || m2.getEndTime() != null;
                    if (m1HasTime && !m2HasTime) return -1;
                    if (!m1HasTime && m2HasTime) return 1;
                    return m1.getId().compareTo(m2.getId());
                })
                .filter(m -> isStaff || isMenuActiveNow(m, now))
                .map(this::toMenuSummaryDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional
    @Override
    public List<MenuWithDishesDto> getActiveMenusWithDishes(String filter) {
        return getActiveMenusWithDishes(filter, List.of(), List.of(), List.of());
    }

    @Transactional
    @Override
    public List<MenuWithDishesDto> getActiveMenusWithDishes(
            String filter,
            List<String> includeTags,
            List<String> excludeTags,
            List<String> excludeAllergens
    ) {
        List<Menu> menus = menuRepository.findAllWithDishes();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }

        LocalTime now = LocalTime.now(appClock).truncatedTo(ChronoUnit.MINUTES);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));

        List<Menu> active = menus.stream()
                .sorted((m1, m2) -> {
                    boolean m1HasTime = m1.getStartTime() != null || m1.getEndTime() != null;
                    boolean m2HasTime = m2.getStartTime() != null || m2.getEndTime() != null;
                    if (m1HasTime && !m2HasTime) return -1;
                    if (!m1HasTime && m2HasTime) return 1;
                    return m1.getId().compareTo(m2.getId());
                })
                .filter(m -> isStaff || isMenuActiveNow(m, now))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Menu> filtered;
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            filtered = active;
        } else {
            filtered = active.stream()
                    .filter(m -> m.getId() != null && m.getId().toString().equals(filter))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        List<MenuWithDishesDto> dtos = filtered.stream()
                .map(menuMapper::toMenuWithDishesDto)
                .collect(Collectors.toCollection(ArrayList::new));

        attachDishTags(dtos);
        attachDishAllergens(dtos);

        applyDishFilters(dtos, includeTags, excludeTags, excludeAllergens);

        java.util.List<com.example.CourseWork.dto.menu.DishResponseDto> allDishes = dtos.stream()
                .filter(m -> m.getDishes() != null)
                .flatMap(m -> m.getDishes().stream())
                .collect(java.util.stream.Collectors.toList());
        dishRatingService.enrichWithRatings(allDishes);

        return dtos;
    }

    private MenuSummaryDto toMenuSummaryDto(Menu menu) {
        MenuSummaryDto dto = new MenuSummaryDto();
        dto.setId(menu.getId());
        dto.setName(menu.getName());
        dto.setStartTime(menu.getStartTime());
        dto.setEndTime(menu.getEndTime());
        return dto;
    }

    /**
     * Load tags for all dishes in one query (avoids N+1 on dish_tags after tags became LAZY).
     */
    private void attachDishTags(List<MenuWithDishesDto> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        List<Integer> dishIds = collectDishIds(menus);
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

    private void attachDishAllergens(List<MenuWithDishesDto> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        List<Integer> dishIds = collectDishIds(menus);
        if (dishIds.isEmpty()) {
            return;
        }

        Map<Integer, List<String>> allergensByDishId = new HashMap<>();
        for (Dish d : dishRepository.findAllByIdWithAllergens(dishIds)) {
            if (d == null || d.getId() == null) {
                continue;
            }
            List<String> allergens = d.getAllergens();
            allergensByDishId.put(d.getId(), allergens == null ? List.of() : new ArrayList<>(allergens));
        }

        for (MenuWithDishesDto menu : menus) {
            if (menu.getDishes() == null) {
                continue;
            }
            for (com.example.CourseWork.dto.menu.DishResponseDto dishDto : menu.getDishes()) {
                if (dishDto == null || dishDto.getId() == null) {
                    continue;
                }
                dishDto.setAllergens(allergensByDishId.getOrDefault(dishDto.getId(), List.of()));
            }
        }
    }

    private static List<Integer> collectDishIds(List<MenuWithDishesDto> menus) {
        return menus.stream()
                .filter(m -> m.getDishes() != null)
                .flatMap(m -> m.getDishes().stream())
                .map(com.example.CourseWork.dto.menu.DishResponseDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private static void applyDishFilters(
            List<MenuWithDishesDto> menus,
            List<String> includeTags,
            List<String> excludeTags,
            List<String> excludeAllergens
    ) {
        Set<String> includeTagSet = normalizeToSet(includeTags);
        Set<String> excludeTagSet = normalizeToSet(excludeTags);
        Set<String> excludeAllergenSet = normalizeToSet(excludeAllergens);

        if (includeTagSet.isEmpty() && excludeTagSet.isEmpty() && excludeAllergenSet.isEmpty()) {
            return;
        }

        for (MenuWithDishesDto menu : menus) {
            if (menu == null || menu.getDishes() == null) {
                continue;
            }
            menu.setDishes(menu.getDishes().stream()
                    .filter(d -> dishMatchesFilters(d, includeTagSet, excludeTagSet, excludeAllergenSet))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
    }

    private static boolean dishMatchesFilters(
            com.example.CourseWork.dto.menu.DishResponseDto dish,
            Set<String> includeTags,
            Set<String> excludeTags,
            Set<String> excludeAllergens
    ) {
        if (dish == null) {
            return false;
        }

        if (!includeTags.isEmpty()) {
            List<String> tags = dish.getTags();
            if (tags == null || tags.stream().noneMatch(includeTags::contains)) {
                return false;
            }
        }

        if (!excludeTags.isEmpty()) {
            List<String> tags = dish.getTags();
            if (tags != null && tags.stream().anyMatch(excludeTags::contains)) {
                return false;
            }
        }

        if (!excludeAllergens.isEmpty()) {
            List<String> allergens = dish.getAllergens();
            if (allergens != null && allergens.stream().anyMatch(excludeAllergens::contains)) {
                return false;
            }
        }

        return true;
    }

    private static Set<String> normalizeToSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
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