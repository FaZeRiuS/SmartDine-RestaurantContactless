package com.example.CourseWork.service.impl;

import com.example.CourseWork.service.SseService;
import com.example.CourseWork.addition.NotificationMessages;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseServiceImpl implements SseService {

    // Map of userId -> list of emitters (a user might have multiple tabs open)
    private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    // List of emitters for staff (dashboards)
    private final List<SseEmitter> staffEmitters = new CopyOnWriteArrayList<>();

    @Value("${app.sse.timeout-ms:3600000}")
    private long sseTimeout;

    /**
     * Some browsers/proxies buffer very small SSE frames. A ~2KB payload helps ensure flushing.
     */
    private static final String HEARTBEAT_PAYLOAD = " ".repeat(2048);

    @Override
    public SseEmitter subscribe(String userId, boolean isStaff) {
        // Use parameterized timeout (default 1 hour)
        SseEmitter emitter = new SseEmitter(sseTimeout);
        
        if (isStaff) {
            staffEmitters.add(emitter);
            emitter.onCompletion(() -> staffEmitters.remove(emitter));
            emitter.onTimeout(() -> staffEmitters.remove(emitter));
            emitter.onError(e -> staffEmitters.remove(emitter));
        } else {
            userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
            emitter.onCompletion(() -> removeUserEmitter(userId, emitter));
            emitter.onTimeout(() -> removeUserEmitter(userId, emitter));
            emitter.onError(e -> removeUserEmitter(userId, emitter));
        }

        // Send an initial "connected" event to verify the stream is open
        try {
            emitter.send(SseEmitter.event()
                    .name(NotificationMessages.SSE_EVENT_CONNECTED)
                    .data(NotificationMessages.SSE_CONNECTED_MESSAGE)
                    .reconnectTime(10000)); // Hint browser to reconnect every 10s if connection lost
        } catch (Exception ignored) {
            // Client disconnected before first frame; emitter cleanup runs via callbacks
        }

        return emitter;
    }

    @Override
    public void sendOrderUpdate(String userId, Object order) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            sendToEmitters(emitters, NotificationMessages.SSE_EVENT_ORDER_UPDATE, order);
        }
        // Staff should also see these updates if they are on the orders page
        sendToEmitters(staffEmitters, NotificationMessages.SSE_EVENT_STAFF_UPDATE,
                NotificationMessages.staffUpdateOrderStatusChangedForUser(userId));
    }

    @Override
    public void sendStaffNotification(String message) {
        sendToEmitters(staffEmitters, NotificationMessages.SSE_EVENT_STAFF_NOTIFICATION, message);
    }
    
    @Override
    public void sendUserNotification(String userId, String message) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            sendToEmitters(emitters, NotificationMessages.SSE_EVENT_ORDER_NOTIFICATION, message);
        }
    }

    @SuppressWarnings("null")
    private void sendToEmitters(List<SseEmitter> emitters, String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception ignored) {
                // Broken emitter; onCompletion/onError will detach it
            }
        }
    }

    private void removeUserEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * Sends a heartbeat comment to all active emitters every 25 seconds.
     * This keeps the connection alive through proxies and prevents timeouts.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        staffEmitters.forEach(this::sendHeartbeat);
        userEmitters.values().forEach(emitters -> emitters.forEach(this::sendHeartbeat));
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            // Use a small named event rather than a comment to improve delivery/flush through intermediaries.
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data(Objects.requireNonNull(HEARTBEAT_PAYLOAD))
                    .reconnectTime(10000));
        } catch (Exception ignored) {
            // Emitter will be removed via onError/onCompletion
        }
    }
}
