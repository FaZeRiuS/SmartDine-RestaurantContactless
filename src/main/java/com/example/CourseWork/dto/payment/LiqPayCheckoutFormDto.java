package com.example.CourseWork.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiqPayCheckoutFormDto {
    private String actionUrl;
    private String data;
    private String signature;
    private String liqpayOrderId;
}

