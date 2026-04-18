package com.example.CourseWork.dto.push;

import lombok.Data;

@Data
public class PushSubscriptionDto {
    private String endpoint;
    private String p256dh;
    private String auth;
}
