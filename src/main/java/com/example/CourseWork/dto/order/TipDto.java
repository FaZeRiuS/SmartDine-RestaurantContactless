package com.example.CourseWork.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipDto {
    @DecimalMin(value = "0.00", inclusive = true, message = "amount must be >= 0")
    @Digits(integer = 17, fraction = 2, message = "amount must have max 2 decimal places")
    private BigDecimal amount;
}

