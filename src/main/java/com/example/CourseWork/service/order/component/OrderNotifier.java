package com.example.CourseWork.service.order.component;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.i18n.NotificationMessages;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.service.push.PushNotificationService;
import com.example.CourseWork.service.sse.SseService;
import org.springframework.stereotype.Component;

@Component
public class OrderNotifier {

    private final SseService sseService;
    private final PushNotificationService pushNotificationService;

    public OrderNotifier(SseService sseService, PushNotificationService pushNotificationService) {
        this.sseService = sseService;
        this.pushNotificationService = pushNotificationService;
    }

    public void notifyUserOfUpdate(String userId, OrderResponseDto order) {
        if (userId == null) {
            return;
        }
        sseService.sendOrderUpdate(userId, order);

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            sseService.sendUserNotification(userId,
                    NotificationMessages.userOrderFinalized(order.getId(), order.getStatus()));
        }
    }

    public void notifyWaitersAboutNewOrder(Integer tableNumber, Integer orderId) {
        pushNotificationService.sendNotificationToRole(NotificationMessages.ROLE_WAITER,
                NotificationMessages.pushWaiterNewOrder(tableNumber));
        sseService.sendStaffNotification(NotificationMessages.staffNewOrderCreated(orderId));
    }

    public void notifyStaffOrderUpdated(Integer orderId, boolean reload) {
        sseService.sendStaffNotification(NotificationMessages.staffOrderUpdated(orderId, reload));
    }

    public void sendStaffNotification(String message) {
        sseService.sendStaffNotification(message);
    }

    public void sendPushToUser(String userId, String jsonPayload) {
        pushNotificationService.sendNotificationToUser(userId, jsonPayload);
    }

    public void sendPushToRole(String role, String jsonPayload) {
        pushNotificationService.sendNotificationToRole(role, jsonPayload);
    }

    public void notifyStatusChange(String userId, OrderResponseDto order, OrderStatus newStatus) {
        notifyUserOfUpdate(userId, order);

        String statusText = NotificationMessages.getLocalizedStatus(newStatus);
        sendPushToUser(userId, NotificationMessages.pushUserOrderStatus(statusText));

        if (newStatus == OrderStatus.READY) {
            sendPushToRole(NotificationMessages.ROLE_WAITER,
                    NotificationMessages.pushWaiterOrderReady(order.getTableNumber()));
        }

        boolean reload = order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED;
        sendStaffNotification(NotificationMessages.staffOrderStatusUpdated(order.getId(), reload));
    }
}

