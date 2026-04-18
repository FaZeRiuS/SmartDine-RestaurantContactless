package com.example.CourseWork.service.order.component;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.model.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderPaymentPolicyTest {

    private final OrderPaymentPolicy policy = new OrderPaymentPolicy(new OrderAccessPolicy());

    @Test
    void assertNotPaid_whenPaid_shouldThrowBadRequest() {
        Order order = new Order();
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        assertThatThrownBy(() -> policy.assertNotPaid(order))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.ORDER_ALREADY_PAID);
    }

    @Test
    void assertReviewable_whenNotPaid_shouldThrowBadRequest() {
        Order order = new Order();
        order.setStatus(OrderStatus.READY);
        order.setPaymentStatus(PaymentStatus.PENDING);

        assertThatThrownBy(() -> policy.assertReviewable(order))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.ORDER_NOT_PAID);
    }

    @Test
    void assertReviewable_whenPaidButWrongStatus_shouldThrowBadRequest() {
        Order order = new Order();
        order.setStatus(OrderStatus.PREPARING);
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        assertThatThrownBy(() -> policy.assertReviewable(order))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.ORDER_NOT_READY_FOR_REVIEW);
    }
}

