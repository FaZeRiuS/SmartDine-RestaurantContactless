package com.example.CourseWork.service;

import com.example.CourseWork.dto.DishResponseDto;

import java.util.List;

public interface DishRatingService {
    void enrichWithRatings(List<DishResponseDto> dishes);
}

