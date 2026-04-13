package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderPaymentServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderNotifier orderNotifier;
    @Mock private OrderPaymentPolicy orderPaymentPolicy;

    @InjectMocks
    private OrderPaymentServiceImpl service;

    @Test
    void payOrder_whenNotFound_shouldThrowNotFound() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.payOrder(1, "u1", "pm"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorMessages.ORDER_NOT_FOUND);
    }

    @Test
    void payOrder_shouldSetPendingAndNotify() {
        Order order = new Order();
        order.setId(1);
        order.setUserId("u1");
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        OrderResponseDto dto = service.payOrder(1, "u1", "pm_test");

        assertThat(dto).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orderPaymentPolicy).assertOwner(order, "u1");
        verify(orderPaymentPolicy).assertNotPaid(order);
        verify(orderNotifier).notifyUserOfUpdate(eq("u1"), any());
    }
}

