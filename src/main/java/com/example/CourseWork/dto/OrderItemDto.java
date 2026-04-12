package com.example.CourseWork.dto;

import lombok.Data;

@Data
public class OrderItemDto {
    private Integer dishId;
    private Integer quantity;
    private String specialRequest;
}
