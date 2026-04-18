package com.example.CourseWork.service.menu;

import com.example.CourseWork.dto.menu.DishResponseDto;

import java.util.List;

public interface DishRatingService {
    void enrichWithRatings(List<DishResponseDto> dishes);
}

