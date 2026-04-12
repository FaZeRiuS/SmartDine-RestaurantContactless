package com.example.CourseWork.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderReviewRequestDto {
    private Integer serviceRating;
    private String comment;
    private List<DishRatingDto> dishRatings;

    @Data
    public static class DishRatingDto {
        private Integer dishId;
        private Integer rating;
    }
}

