package com.example.CourseWork.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CartItemDto {
    @NotNull(message = "dishId is required")
    private Integer dishId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private Integer quantity;
    private String specialRequest;
}
