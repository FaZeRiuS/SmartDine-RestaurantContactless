package com.example.CourseWork.service.recommendation.impl;

import com.example.CourseWork.dto.menu.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.menu.DishRatingService;
import com.example.CourseWork.service.user.UserPreferenceService;
import com.example.CourseWork.security.CurrentUserIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private DishRepository dishRepository;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private DishRatingService dishRatingService;

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void getRecommendations_ShouldReturnEnrichedDishes() {
        // Arrange
        String userId = "user-123";
        Dish dish = new Dish();
        dish.setId(1);
        when(dishRepository.findRecommendedDishes(userId)).thenReturn(List.of(dish));
        when(dishMapper.toResponseDto(any())).thenReturn(new DishResponseDto());
        when(dishRepository.findAllByIdWithAllergens(any())).thenReturn(List.of(dish));
        when(dishRepository.findAllByIdWithTags(any())).thenReturn(List.of(dish));
        when(userPreferenceService.getExcludedAllergens(userId)).thenReturn(java.util.Set.of());

        // Act
        List<DishResponseDto> result = recommendationService.getRecommendations(userId);

        // Assert
        assertEquals(1, result.size());
        verify(dishRatingService).enrichWithRatings(any());
    }

    @Test
    void getRecommendations_ShouldFilterExcludedAllergens() {
        String userId = "user-123";
        Dish dish = new Dish();
        dish.setId(1);
        dish.setAllergens(List.of("milk"));
        dish.setTags(List.of("dessert"));

        when(dishRepository.findRecommendedDishes(userId)).thenReturn(List.of(dish));
        when(dishRepository.findAllByIdWithAllergens(any())).thenReturn(List.of(dish));
        when(dishRepository.findAllByIdWithTags(any())).thenReturn(List.of(dish));
        DishResponseDto dto = new DishResponseDto();
        dto.setId(1);
        when(dishMapper.toResponseDto(any())).thenReturn(dto);
        when(userPreferenceService.getExcludedAllergens(userId)).thenReturn(java.util.Set.of("milk"));

        List<DishResponseDto> result = recommendationService.getRecommendations(userId);
        assertEquals(0, result.size());
    }
}
