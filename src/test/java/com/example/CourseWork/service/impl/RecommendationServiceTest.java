package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.DishRatingService;
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

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    void getRecommendations_ShouldReturnEnrichedDishes() {
        // Arrange
        String userId = "user-123";
        com.example.CourseWork.model.Dish dish = new com.example.CourseWork.model.Dish();
        when(dishRepository.findRecommendedDishes(userId)).thenReturn(List.of(dish));
        when(dishMapper.toResponseDto(any())).thenReturn(new DishResponseDto());

        // Act
        List<DishResponseDto> result = recommendationService.getRecommendations(userId);

        // Assert
        assertEquals(1, result.size());
        verify(dishRatingService).enrichWithRatings(any());
    }
}
