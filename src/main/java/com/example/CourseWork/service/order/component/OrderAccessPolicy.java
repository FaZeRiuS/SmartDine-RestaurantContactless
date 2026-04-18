package com.example.CourseWork.service.order.component;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.model.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OrderAccessPolicy {

    public boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR")
                        || a.getAuthority().equals("ROLE_CHEF")
                        || a.getAuthority().equals("ROLE_WAITER"));
    }

    public void assertOwner(Order order, String userId) {
        if (order == null) {
            throw new NotFoundException(ErrorMessages.ORDER_NOT_FOUND);
        }
        if (userId == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorMessages.ACCESS_DENIED);
        }
    }

    /**
     * Rules for adding items to an existing order.
     * Allows adding to NEW/PREPARING/READY (READY will typically transition to PREPARING in service),
     * but never for paid, cancelled, or completed orders.
     */
    public void assertAddItemsAllowed(Order order) {
        if (order == null) {
            throw new NotFoundException(ErrorMessages.ORDER_NOT_FOUND);
        }
        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new BadRequestException(ErrorMessages.ORDER_ITEMS_NOT_MODIFIABLE);
        }
        if (OrderStatus.CANCELLED.equals(order.getStatus()) || OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new BadRequestException(ErrorMessages.ORDER_ITEMS_NOT_MODIFIABLE);
        }
    }

    public void assertModifiableItems(Order order) {
        if (order.getStatus() != OrderStatus.NEW || PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new BadRequestException(ErrorMessages.ORDER_ITEMS_NOT_MODIFIABLE);
        }
    }

    /**
     * Increasing quantity may be allowed for non-NEW statuses in this codebase,
     * but decreasing/removing is restricted to strictly modifiable orders.
     */
    public void assertDecreaseOrRemovalAllowed(Order order) {
        assertModifiableItems(order);
    }

    public void assertUpdateItemQuantityAllowed(Order order, int oldQuantity, int newQuantity) {
        if (newQuantity < oldQuantity) {
            assertDecreaseOrRemovalAllowed(order);
        }
    }
}

