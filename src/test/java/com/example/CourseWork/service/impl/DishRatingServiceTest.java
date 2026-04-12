package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishRatingServiceTest {

    @Mock
    private OrderDishReviewRepository orderDishReviewRepository;

    @InjectMocks
    private DishRatingServiceImpl dishRatingService;

    @Test
    void enrichWithRatings_ShouldSetRatings_WhenFound() {
        // Arrange
        DishResponseDto dish1 = new DishResponseDto();
        dish1.setId(1);
        DishResponseDto dish2 = new DishResponseDto();
        dish2.setId(2);

        OrderDishReviewRepository.DishRatingAgg agg1 = mock(OrderDishReviewRepository.DishRatingAgg.class);
        when(agg1.getDishId()).thenReturn(1);
        when(agg1.getAvgRating()).thenReturn(4.5);
        when(agg1.getRatingsCount()).thenReturn(10L);

        when(orderDishReviewRepository.aggregateRatingsForDishIds(anyList()))
                .thenReturn(List.of(agg1));

        // Act
        dishRatingService.enrichWithRatings(List.of(dish1, dish2));

        // Assert
        assertEquals(4.5, dish1.getAvgRating());
        assertEquals(10L, dish1.getRatingsCount());
        
        assertNull(dish2.getAvgRating());
        assertEquals(0L, dish2.getRatingsCount());
    }

    @Test
    void enrichWithRatings_ShouldDoNothing_WhenEmptyList() {
        // Act
        dishRatingService.enrichWithRatings(List.of());

        // Assert
        // No exceptions and no repo calls expected (verified by Mockito if needed)
    }
}
