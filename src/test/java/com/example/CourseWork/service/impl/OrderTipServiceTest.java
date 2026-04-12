package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderTipServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderTipServiceImpl orderTipService;

    @NonNull
    private Integer orderId = 1;
    @NonNull
    private UUID userId = UUID.randomUUID();
    @NonNull
    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(orderId);
        order.setUserId(userId.toString());
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Test
    void setTip_ShouldSucceed_WhenValidAmount() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        orderTipService.setTip(orderId, userId, new BigDecimal("50.00"));

        // Assert
        assertEquals(new BigDecimal("50.00"), order.getTipAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void setTip_ShouldCap_WhenAmountExceedsMax() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        orderTipService.setTip(orderId, userId, new BigDecimal("20000.00"));

        // Assert
        assertEquals(new BigDecimal("10000.00"), order.getTipAmount());
    }

    @Test
    void setTip_ShouldThrowException_WhenOrderAlreadyPaid() {
        // Arrange
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderTipService.setTip(orderId, userId, BigDecimal.TEN));
    }

    @Test
    void setTip_ShouldThrowException_WhenUnauthorized() {
        // Arrange
        order.setUserId("other-user");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderTipService.setTip(orderId, userId, BigDecimal.TEN));
    }
}
