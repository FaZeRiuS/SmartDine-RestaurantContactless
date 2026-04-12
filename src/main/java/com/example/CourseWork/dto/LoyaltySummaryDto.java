package com.example.CourseWork.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LoyaltySummaryDto {
    private UUID userId;
    private BigDecimal balance;
    private BigDecimal cashbackRate;
    private Long successfulOrdersCount;
    private BigDecimal nextRate;
    private Long ordersToNextRate;
}

