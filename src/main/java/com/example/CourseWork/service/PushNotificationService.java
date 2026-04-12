package com.example.CourseWork.service;

public interface PushNotificationService {
    void sendNotificationToUser(String userId, String payload);
    void sendNotificationToRole(String role, String payload);
}
