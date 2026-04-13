package com.example.CourseWork.service.order.component;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentPolicy {

    private final OrderAccessPolicy orderAccessPolicy;

    public OrderPaymentPolicy(OrderAccessPolicy orderAccessPolicy) {
        this.orderAccessPolicy = orderAccessPolicy;
    }

    public void assertOwner(Order order, String userId) {
        orderAccessPolicy.assertOwner(order, userId);
    }

    public void assertNotPaid(Order order) {
        if (PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new BadRequestException(ErrorMessages.ORDER_ALREADY_PAID);
        }
    }

    public void assertPaid(Order order) {
        if (!PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new BadRequestException(ErrorMessages.ORDER_NOT_PAID);
        }
    }

    public void assertReviewable(Order order) {
        assertPaid(order);
        if (!(OrderStatus.READY.equals(order.getStatus()) || OrderStatus.COMPLETED.equals(order.getStatus()))) {
            throw new BadRequestException(ErrorMessages.ORDER_NOT_READY_FOR_REVIEW);
        }
    }
}

