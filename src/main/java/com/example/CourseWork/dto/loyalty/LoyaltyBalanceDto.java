package com.example.CourseWork.dto.loyalty;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LoyaltyBalanceDto {
    private UUID userId;
    private BigDecimal balance;
}

