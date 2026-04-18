package com.example.CourseWork.i18n;

import com.example.CourseWork.model.OrderStatus;

public final class NotificationMessages {
    private NotificationMessages() {}

    // SSE event names and common data
    public static final String SSE_EVENT_CONNECTED = "connected";
    public static final String SSE_CONNECTED_MESSAGE = "SSE connection established";
    public static final String SSE_EVENT_ORDER_UPDATE = "order-update";
    public static final String SSE_EVENT_STAFF_UPDATE = "staff-update";
    public static final String SSE_EVENT_STAFF_NOTIFICATION = "staff-notification";
    public static final String SSE_EVENT_ORDER_NOTIFICATION = "order-notification";
    public static final String SSE_RELOAD_PREFIX = "[RELOAD] ";

    // Staff notifications (SSE)
    public static String staffNewOrderCreated(Integer orderId) {
        return "New order created: " + orderId;
    }

    public static String staffOrderUpdated(Integer orderId, boolean reload) {
        return (reload ? SSE_RELOAD_PREFIX : "") + "Order updated: " + orderId;
    }

    public static String staffOrderStatusUpdated(Integer orderId, boolean reload) {
        return (reload ? SSE_RELOAD_PREFIX : "") + "Order status updated: " + orderId;
    }

    public static String staffWaiterCalled(Integer tableNumber) {
        return "Waiter called for Table #" + tableNumber;
    }

    // User notifications (SSE)
    public static String userOrderFinalized(Integer orderId, OrderStatus status) {
        return SSE_RELOAD_PREFIX + "Order " + orderId + " is finalized (" + status + ")";
    }

    // Push payload builders (JSON strings; existing services expect String)
    public static String pushWaiterNewOrder(Integer tableNumber) {
        return "{\"title\": \"Нове замовлення!\", \"body\": \"Стіл №" + tableNumber + "\", \"url\": \"/staff/orders\"}";
    }

    public static final String ROLE_WAITER = "ROLE_WAITER";

    public static String pushWaiterNeeded(Integer tableNumber) {
        return "{\"title\": \"Потрібен офіціант!\", \"body\": \"Стіл №" + tableNumber + "\", \"url\": \"/staff/orders\"}";
    }

    public static String pushWaiterOrderReady(Integer tableNumber) {
        return "{\"title\": \"Замовлення готове!\", \"body\": \"Стіл №" + tableNumber + "\", \"url\": \"/staff/orders\"}";
    }

    public static String pushUserOrderStatus(String statusText) {
        return pushUserOrderStatus(statusText, "/orders");
    }

    public static String pushUserOrderStatus(String statusText, String url) {
        String safeUrl = (url == null || url.isBlank()) ? "/" : url;
        return "{\"title\": \"SmartDine\", \"body\": \"" + statusText + "\", \"url\": \"" + safeUrl + "\"}";
    }

    // SSE staff update payloads
    public static String staffUpdateOrderStatusChangedForUser(String userId) {
        return "Order status changed for user: " + userId;
    }

    public static String getLocalizedStatus(OrderStatus status) {
        return switch (status) {
            case NEW -> "Замовлення прийнято";
            case PREPARING -> "Замовлення готується";
            case READY -> "Замовлення готове! Смачного!";
            case COMPLETED -> "Замовлення завершено. Дякуємо!";
            case CANCELLED -> "Замовлення скасовано";
        };
    }
}
