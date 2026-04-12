package com.example.CourseWork.dto;

import lombok.Data;

@Data
public class CartItemDetailDto {
    private Integer id;
    private Integer dishId;
    private String dishName;
    private Float price;
    private Integer quantity;
    private String specialRequest;
}
