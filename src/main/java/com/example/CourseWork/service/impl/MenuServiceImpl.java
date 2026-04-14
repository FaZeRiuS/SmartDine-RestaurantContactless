package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.*;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.MenuMapper;
import com.example.CourseWork.model.Menu;
import com.example.CourseWork.repository.MenuRepository;
import com.example.CourseWork.service.MenuService;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalTime;

import com.example.CourseWork.service.RecommendationService;
import com.example.CourseWork.service.DishRatingService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final RecommendationService recommendationService;
    private final DishRatingService dishRatingService;
    private final CurrentUserIdentity currentUserIdentity;

    @Transactional
    @Override
    @Cacheable(cacheNames = "menusWithDishes", key = "@publicPageCacheKey.menusWithDishesKey()")
    public List<MenuWithDishesDto> getAllMenusWithDishes() {
        List<Menu> menus = menuRepository.findAll();
        if (menus.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalTime now = LocalTime.now();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHEF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));

        List<MenuWithDishesDto> dtos = menus.stream()
                .sorted((m1, m2) -> {
                    boolean m1HasTime = m1.getStartTime() != null || m1.getEndTime() != null;
                    boolean m2HasTime = m2.getStartTime() != null || m2.getEndTime() != null;
                    if (m1HasTime && !m2HasTime) return -1;
                    if (!m1HasTime && m2HasTime) return 1;
                    return m1.getId().compareTo(m2.getId());
                })
                .filter(menu -> isStaff || isMenuActiveNow(menu, now))
                .map(menuMapper::toMenuWithDishesDto)
                .collect(Collectors.toCollection(ArrayList::new));
                
        // Inject recommendations menu for customer UI only (never for staff/admin).
        if (!isStaff) {
            if (!currentUserIdentity.isGuest()) {
                String userId = currentUserIdentity.currentUserId();
                List<DishResponseDto> recommendedDishes = recommendationService.getRecommendations(userId);
                if (recommendedDishes != null && !recommendedDishes.isEmpty()) {
                    MenuWithDishesDto recommendationsMenu = new MenuWithDishesDto();
                    recommendationsMenu.setId(-1);
                    recommendationsMenu.setName("Recommendations for You");
                    recommendationsMenu.setDishes(recommendedDishes);
                    // Add to the front so it's the first menu they see
                    dtos.add(0, recommendationsMenu);
                }
            } else {
                // For anonymous users, get popular dishes using a dummy ID to bypass user tag matching but keep popularity scoring
                List<DishResponseDto> popularDishes = recommendationService.getRecommendations("anonymous-user");
                if (popularDishes != null && !popularDishes.isEmpty()) {
                    MenuWithDishesDto popularMenu = new MenuWithDishesDto();
                    popularMenu.setId(-2);
                    popularMenu.setName("Popular Dishes");
                    popularMenu.setDishes(popularDishes);
                    dtos.add(0, popularMenu);
                }
            }
        }

        // Enrich all dishes in all menus with aggregated ratings (avg + count)
        java.util.List<com.example.CourseWork.dto.DishResponseDto> allDishes = dtos.stream()
                .filter(m -> m.getDishes() != null)
                .flatMap(m -> m.getDishes().stream())
                .collect(java.util.stream.Collectors.toList());
        dishRatingService.enrichWithRatings(allDishes);

        return dtos;
    }

    @Override
    @CacheEvict(cacheNames = {"menusWithDishes", "availableDishes"}, allEntries = true)
    public MenuResponseDto createMenu(MenuDto dto) {
        Menu menu = new Menu();
        menu.setName(dto.getName());
        menu.setStartTime(dto.getStartTime());
        menu.setEndTime(dto.getEndTime());
        return menuMapper.toResponseDto(menuRepository.save(menu));
    }

    @Override
    @CacheEvict(cacheNames = {"menusWithDishes", "availableDishes"}, allEntries = true)
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
    @SuppressWarnings("null")
    @CacheEvict(cacheNames = {"menusWithDishes", "availableDishes"}, allEntries = true)
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