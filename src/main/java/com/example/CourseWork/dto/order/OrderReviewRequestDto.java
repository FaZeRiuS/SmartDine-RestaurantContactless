package com.example.CourseWork.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderReviewRequestDto {
    @NotNull(message = "serviceRating is required")
    @Min(value = 1, message = "serviceRating must be between 1 and 5")
    @Max(value = 5, message = "serviceRating must be between 1 and 5")
    private Integer serviceRating;
    private String comment;

    @Valid
    private List<DishRatingDto> dishRatings;

    @Data
    public static class DishRatingDto {
        @NotNull(message = "dishId is required")
        private Integer dishId;

        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be between 1 and 5")
        @Max(value = 5, message = "rating must be between 1 and 5")
        private Integer rating;
    }
}

