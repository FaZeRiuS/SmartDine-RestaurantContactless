package com.example.CourseWork.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartResponseDto {
    private Integer cartId;
    private String userId;
    private List<CartItemDetailDto> items;
}
