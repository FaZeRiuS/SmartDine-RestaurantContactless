package com.example.CourseWork.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LiqPayCallbackDto {
    private String status;

    @JsonProperty("order_id")
    private String orderId;

    private String amount;
    private String currency;
}

