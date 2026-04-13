package com.example.CourseWork.service.order.component;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.model.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAccessPolicyTest {

    private final OrderAccessPolicy policy = new OrderAccessPolicy();

    @Test
    void assertOwner_whenNotOwner_shouldThrowForbidden() {
        Order order = new Order();
        order.setUserId("user-1");

        assertThatThrownBy(() -> policy.assertOwner(order, "user-2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessages.ACCESS_DENIED);
    }

    @Test
    void assertAddItemsAllowed_whenPaid_shouldThrowBadRequest() {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.SUCCESS);

        assertThatThrownBy(() -> policy.assertAddItemsAllowed(order))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.ORDER_ITEMS_NOT_MODIFIABLE);
    }

    @Test
    void assertModifiableItems_whenNotNew_shouldThrowBadRequest() {
        Order order = new Order();
        order.setStatus(OrderStatus.PREPARING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        assertThatThrownBy(() -> policy.assertModifiableItems(order))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.ORDER_ITEMS_NOT_MODIFIABLE);
    }
}

