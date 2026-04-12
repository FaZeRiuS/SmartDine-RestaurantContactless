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
}
