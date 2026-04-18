package com.example.CourseWork.dto.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemResponseDto {
    private Integer id;
    private Integer dishId;
    private String dishName;
    private Integer quantity;
    private BigDecimal price;
    private String specialRequest;
    private Integer rating;
}
