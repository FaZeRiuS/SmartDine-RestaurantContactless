package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.DishResponseDto;
import com.example.CourseWork.mapper.DishMapper;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.service.DishRatingService;
import com.example.CourseWork.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final DishRepository dishRepository;
    private final DishMapper dishMapper;
    private final DishRatingService dishRatingService;

    @Override
    @Transactional(readOnly = true)
    public List<DishResponseDto> getRecommendations(String userId) {
        List<DishResponseDto> dishes = dishRepository.findRecommendedDishes(userId).stream()
                .map(dishMapper::toResponseDto)
                .collect(Collectors.toList());
        dishRatingService.enrichWithRatings(dishes);
        return dishes;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DishResponseDto> getCrossSellRecommendation(Integer baseDishId, java.util.List<Integer> existingDishIds) {
        java.util.List<Integer> existing = existingDishIds != null ? existingDishIds : java.util.List.of();

        var result = dishRepository.findSmartComboForDish(baseDishId);

        if (result.isEmpty()) {
            result = dishRepository.findPopularComboFallback(baseDishId);
        }

        if (result.isPresent() && existing.contains(result.get().getId())) {
            return java.util.Optional.empty();
        }

        java.util.Optional<DishResponseDto> dto = result.map(dishMapper::toResponseDto);
        dto.ifPresent(d -> dishRatingService.enrichWithRatings(java.util.List.of(d)));
        return dto;
    }
}
