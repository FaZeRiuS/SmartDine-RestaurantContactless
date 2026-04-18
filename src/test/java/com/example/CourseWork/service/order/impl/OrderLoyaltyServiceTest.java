package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.loyalty.LoyaltyService;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
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
class OrderLoyaltyServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private LoyaltyService loyaltyService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderPaymentPolicy orderPaymentPolicy;

    @InjectMocks
    private OrderLoyaltyServiceImpl orderLoyaltyService;

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
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setLoyaltyDiscount(BigDecimal.ZERO);
    }

    @Test
    void applyCoverage_ShouldSpendPoints_WhenIncreasingCoverage() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        // Max coverage is 50.00 (50% of 100). We want 30.00.
        orderLoyaltyService.applyCoverage(orderId, userId, new BigDecimal("30.00"));

        // Assert
        assertEquals(new BigDecimal("30.00"), order.getLoyaltyDiscount());
        verify(loyaltyService).spendPointsInternal(eq(userId), eq(new BigDecimal("30.00")), anyString());
    }

    @Test
    void applyCoverage_ShouldRefundPoints_WhenDecreasingCoverage() {
        // Arrange
        order.setLoyaltyDiscount(new BigDecimal("30.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        // Decrease from 30.00 to 10.00 -> refund 20.00
        orderLoyaltyService.applyCoverage(orderId, userId, new BigDecimal("10.00"));

        // Assert
        assertEquals(new BigDecimal("10.00"), order.getLoyaltyDiscount());
        verify(loyaltyService).creditPointsInternal(eq(userId), eq(new BigDecimal("20.00")), anyString());
    }

    @Test
    void applyCoverage_ShouldCapAtMaxRate() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        // Act
        // Requested 80.00, but max is 50.00 (50% of 100)
        orderLoyaltyService.applyCoverage(orderId, userId, new BigDecimal("80.00"));

        // Assert
        assertEquals(new BigDecimal("50.00"), order.getLoyaltyDiscount());
    }

    @Test
    void applyCoverage_ShouldThrowException_WhenOrderPaid() {
        // Arrange
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new BadRequestException(ErrorMessages.ORDER_ALREADY_PAID))
                .when(orderPaymentPolicy).assertNotPaid(order);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderLoyaltyService.applyCoverage(orderId, userId, BigDecimal.TEN));
        assertEquals(ErrorMessages.ORDER_ALREADY_PAID, ex.getMessage());
    }
}
