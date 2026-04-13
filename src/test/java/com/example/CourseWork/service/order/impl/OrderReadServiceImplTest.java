package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderServiceReviewRepository;
import com.example.CourseWork.service.order.component.OrderAccessPolicy;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderReadServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderAccessPolicy orderAccessPolicy;
    @Mock private OrderNotifier orderNotifier;
    @Mock private OrderServiceReviewRepository orderServiceReviewRepository;
    @Mock private OrderDishReviewRepository orderDishReviewRepository;
    @Mock private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private OrderReadServiceImpl service;

    @Test
    void getOrderById_whenNotFound_shouldThrowNotFound() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderById(1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(ErrorMessages.ORDER_NOT_FOUND);
    }

    @Test
    void getOrderById_whenNotOwnerAndNotStaff_shouldThrowForbidden() {
        Order order = new Order();
        order.setId(1);
        order.setUserId("owner");
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(currentUserIdentity.currentUserId()).thenReturn("other");
        when(orderAccessPolicy.isStaff()).thenReturn(false);

        assertThatThrownBy(() -> service.getOrderById(1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage(ErrorMessages.ACCESS_DENIED);
    }

    @Test
    void getNewOrders_whenEmpty_shouldReturnEmptyList() {
        when(orderRepository.findAllByStatusOrderByCreatedAtDesc(any())).thenReturn(List.of());

        List<OrderResponseDto> result = service.getNewOrders();

        assertThat(result).isEmpty();
        verify(orderMapper, never()).toResponseDto(any());
    }
}

