package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Cart;
import com.example.CourseWork.repository.CartRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.component.OrderAccessPolicy;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.order.component.OrderTotalCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderItemServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DishRepository dishRepository;
    @Mock private CartRepository cartRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderTotalCalculator orderTotalCalculator;
    @Mock private OrderAccessPolicy orderAccessPolicy;
    @Mock private OrderNotifier orderNotifier;

    @InjectMocks
    private OrderItemServiceImpl service;

    @Test
    void confirmOrderFromCart_whenCartMissing_shouldThrowNotFound() {
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmOrderFromCart("u1", 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorMessages.CART_NOT_FOUND);
    }

    @Test
    void confirmOrderFromCart_whenCartEmpty_shouldThrowBadRequest() {
        Cart cart = new Cart();
        cart.setUserId("u1");
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.confirmOrderFromCart("u1", 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(ErrorMessages.CART_EMPTY);
    }

    @Test
    void confirmOrderFromCart_whenOk_shouldSaveOrder() {
        Cart cart = new Cart();
        cart.setUserId("u1");
        cart.setItems(new ArrayList<>());
        // add one dummy item
        cart.getItems().add(new com.example.CourseWork.model.CartItem());
        when(cartRepository.findByUserId("u1")).thenReturn(Optional.of(cart));

        when(orderTotalCalculator.calculateTotal(any())).thenReturn(BigDecimal.ZERO);
        when(orderMapper.toResponseDto(any())).thenReturn(new OrderResponseDto());

        service.confirmOrderFromCart("u1", 10);

        verify(orderRepository).save(any());
        verify(cartRepository).save(cart);
        verify(orderNotifier).notifyWaitersAboutNewOrder(eq(10), any());
    }
}

