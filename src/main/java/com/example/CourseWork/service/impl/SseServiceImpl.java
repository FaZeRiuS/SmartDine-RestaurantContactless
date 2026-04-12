package com.example.CourseWork.service.impl;

import com.example.CourseWork.service.SseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseServiceImpl implements SseService {

    // Map of userId -> list of emitters (a user might have multiple tabs open)
    private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    // List of emitters for staff (dashboards)
    private final List<SseEmitter> staffEmitters = new CopyOnWriteArrayList<>();

    @Value("${app.sse.timeout-ms:3600000}")
    private Long sseTimeout;

    @Override
    @SuppressWarnings("null")
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
                    .name("connected")
                    .data("SSE connection established")
                    .reconnectTime(10000)); // Hint browser to reconnect every 10s if connection lost
        } catch (IOException ignored) {
            // Client disconnected before first frame; emitter cleanup runs via callbacks
        }

        return emitter;
    }

    @Override
    public void sendOrderUpdate(String userId, Object order) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            sendToEmitters(emitters, "order-update", order);
        }
        // Staff should also see these updates if they are on the orders page
        sendToEmitters(staffEmitters, "staff-update", "Order status changed for user: " + userId);
    }

    @Override
    public void sendStaffNotification(String message) {
        sendToEmitters(staffEmitters, "staff-notification", message);
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
            // SSE comment line (ignored by EventSource). Non-empty comment avoids edge cases with empty frames.
            emitter.send(SseEmitter.event().comment("keep-alive"));
        } catch (Exception ignored) {
            // Emitter will be removed via onError/onCompletion
        }
    }
}
