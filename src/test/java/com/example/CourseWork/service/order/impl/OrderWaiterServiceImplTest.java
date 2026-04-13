package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.component.OrderNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderWaiterServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderNotifier orderNotifier;

    @InjectMocks
    private OrderWaiterServiceImpl service;

    @Test
    void callWaiter_whenNotFound_shouldThrowNotFound() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.callWaiter(1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorMessages.ORDER_NOT_FOUND);
    }

    @Test
    void callWaiter_whenCompleted_shouldThrowBadRequest() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.callWaiter(1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot call waiter");
    }

    @Test
    void dismissWaiterCall_whenOk_shouldReturnResponse() {
        Order order = new Order();
        order.setId(1);
        order.setUserId("u1");
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        service.dismissWaiterCall(1);
    }
}

