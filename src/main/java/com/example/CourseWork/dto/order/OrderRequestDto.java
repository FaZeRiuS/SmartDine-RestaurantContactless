package com.example.CourseWork.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequestDto {
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<OrderItemDto> items;
}
