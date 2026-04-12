package com.example.CourseWork.dto;

import lombok.Data;

@Data
public class OrderItemResponseDto {
    private Integer id;
    private Integer dishId;
    private String dishName;
    private Integer quantity;
    private float price;
    private String specialRequest;
    private Integer rating;
}
