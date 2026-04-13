package com.example.CourseWork.exception;

public final class ErrorMessages {
    private ErrorMessages() {}

    // Generic
    public static final String ACCESS_DENIED = "Access denied";
    public static final String ONLY_STAFF_CAN_UPDATE_ORDER_STATUS = "Only staff can update order status";
    public static final String BAD_REQUEST = "Bad request";

    // Common required fields
    public static final String ORDER_ID_REQUIRED = "orderId is required";
    public static final String USER_ID_REQUIRED = "userId is required";
    public static final String AMOUNT_REQUIRED = "amount is required";
    public static final String POINTS_REQUIRED = "points is required";
    public static final String ORDER_AMOUNT_REQUIRED = "orderAmount is required";
    public static final String REVIEW_BODY_REQUIRED = "review body is required";

    // Common not found
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String CART_NOT_FOUND = "Cart not found";
    public static final String CART_ITEM_NOT_FOUND = "Cart item not found";
    public static final String ORDER_ITEM_NOT_FOUND = "Order item not found";
    public static final String DISH_NOT_FOUND = "Dish not found";
    public static final String MENU_NOT_FOUND = "Menu not found";
    public static final String MENUS_NOT_FOUND = "Menus not found";

    // Order rules / payment
    public static final String ORDER_ALREADY_PAID = "Order is already paid";
    public static final String ORDER_NOT_PAID = "Order is not paid";
    public static final String ORDER_NOT_READY_FOR_REVIEW = "Order is not ready for review";
    public static final String ORDER_ITEMS_NOT_MODIFIABLE = "Order items are not modifiable";
    public static final String CANNOT_CALL_WAITER_FOR_COMPLETED_OR_CANCELLED = "Cannot call waiter for a completed or cancelled order";
    public static final String INVALID_SERVICE_RATING = "Invalid service rating";
    public static final String INVALID_DISH_RATING = "Invalid dish rating";

    // Cart / dish rules
    public static final String CART_EMPTY = "Cart is empty";
    public static final String INVALID_QUANTITY = "Invalid quantity";
    public static final String DISH_NOT_AVAILABLE = "Dish is not available";

    // Integrations
    public static final String INVALID_LIQPAY_CALLBACK_DATA = "Invalid LiqPay callback data";
    public static final String INVALID_LIQPAY_ORDER_ID_FORMAT_PREFIX = "Invalid order_id format: ";

    // Identity
    public static final String COULD_NOT_IDENTIFY_USER_OR_GUEST_SESSION = "Could not identify user or guest session";
}

