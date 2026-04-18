package com.example.CourseWork.dto.order;

import lombok.Data;

@Data
public class OrderItemDto {
    private Integer dishId;
    private Integer quantity;
    private String specialRequest;
}
