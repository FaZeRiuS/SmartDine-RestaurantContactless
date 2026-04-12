package com.example.CourseWork.service;

import com.example.CourseWork.dto.DishResponseDto;

import java.util.List;

public interface RecommendationService {
    List<DishResponseDto> getRecommendations(String userId);
}
