package com.example.CourseWork.dto.cart;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDetailDto {
    private Integer id;
    private Integer dishId;
    private String dishName;
    private BigDecimal price;
    private Integer quantity;
    private String specialRequest;
}
