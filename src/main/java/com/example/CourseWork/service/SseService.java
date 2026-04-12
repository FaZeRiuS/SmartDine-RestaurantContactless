package com.example.CourseWork.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    /**
     * Subscribes a user or staff member to the real-time event stream.
     * @param userId The unique ID of the user.
     * @param isStaff Whether the subscriber has staff privileges.
     * @return An SseEmitter instance.
     */
    SseEmitter subscribe(String userId, boolean isStaff);

    /**
     * Broadcasts an order update event to a specific user and all staff.
     * @param userId The ID of the order owner.
     * @param order The order data to send.
     */
    void sendOrderUpdate(String userId, Object order);

    /**
     * Broadcasts a notification message to all staff members.
     * @param message The message content.
     */
    void sendStaffNotification(String message);
}
