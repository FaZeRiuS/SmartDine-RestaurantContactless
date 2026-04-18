package com.example.CourseWork.service.push;

public interface PushNotificationService {
    void sendNotificationToUser(String userId, String payload);
    void sendNotificationToRole(String role, String payload);
}
