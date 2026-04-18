package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderReviewRequestDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderServiceReviewRepository;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderReviewServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DishRepository dishRepository;
    @Mock
    private OrderServiceReviewRepository orderServiceReviewRepository;
    @Mock
    private OrderDishReviewRepository orderDishReviewRepository;
    @Mock
    private OrderPaymentPolicy orderPaymentPolicy;

    @InjectMocks
    private OrderReviewServiceImpl orderReviewService;

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
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.COMPLETED);
    }

    @Test
    void submitReview_ShouldSucceed_WhenValidRequest() {
        // Arrange
        Dish dish = new Dish();
        dish.setId(10);
        OrderItem item = new OrderItem();
        item.setDish(dish);
        order.setItems(List.of(item));

        when(orderRepository.findByIdWithItemsAndDishes(orderId)).thenReturn(Optional.of(order));
        when(orderServiceReviewRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(dishRepository.findById(10)).thenReturn(Optional.of(dish));

        OrderReviewRequestDto dto = new OrderReviewRequestDto();
        dto.setServiceRating(5);
        dto.setComment("Great service!");
        var dr = new OrderReviewRequestDto.DishRatingDto();
        dr.setDishId(10);
        dr.setRating(4);
        dto.setDishRatings(List.of(dr));

        // Act
        orderReviewService.submitReview(orderId, userId, dto);

        // Assert
        verify(orderServiceReviewRepository).save(any());
        verify(orderDishReviewRepository).deleteAllByOrderId(orderId);
        verify(orderDishReviewRepository).save(any());
    }

    @Test
    void submitReview_ShouldThrowException_WhenOrderNotPaid() {
        // Arrange
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findByIdWithItemsAndDishes(orderId)).thenReturn(Optional.of(order));
        doThrow(new BadRequestException(ErrorMessages.ORDER_NOT_PAID))
                .when(orderPaymentPolicy).assertReviewable(order);
        OrderReviewRequestDto dto = new OrderReviewRequestDto();

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderReviewService.submitReview(orderId, userId, dto));
        assertEquals(ErrorMessages.ORDER_NOT_PAID, ex.getMessage());
    }

    @Test
    void submitReview_ShouldThrowException_WhenOrderNotReadyOrCompleted() {
        // Arrange
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findByIdWithItemsAndDishes(orderId)).thenReturn(Optional.of(order));
        doThrow(new BadRequestException(ErrorMessages.ORDER_NOT_READY_FOR_REVIEW))
                .when(orderPaymentPolicy).assertReviewable(order);
        OrderReviewRequestDto dto = new OrderReviewRequestDto();

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> orderReviewService.submitReview(orderId, userId, dto));
        assertEquals(ErrorMessages.ORDER_NOT_READY_FOR_REVIEW, ex.getMessage());
    }

    @Test
    void submitReview_ShouldThrowException_WhenUserUnauthorized() {
        // Arrange
        order.setUserId(UUID.randomUUID().toString());
        when(orderRepository.findByIdWithItemsAndDishes(orderId)).thenReturn(Optional.of(order));
        doThrow(new ForbiddenException(ErrorMessages.ACCESS_DENIED))
                .when(orderPaymentPolicy).assertOwner(order, userId.toString());
        OrderReviewRequestDto dto = new OrderReviewRequestDto();

        // Act & Assert
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> orderReviewService.submitReview(orderId, userId, dto));
        assertEquals(ErrorMessages.ACCESS_DENIED, ex.getMessage());
    }

    @Test
    void submitReview_ShouldIgnoreInvalidDish_WhenDishNotInOrder() {
        // Arrange
        order.setItems(List.of()); // No items
        when(orderRepository.findByIdWithItemsAndDishes(orderId)).thenReturn(Optional.of(order));
        when(orderServiceReviewRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        OrderReviewRequestDto dto = new OrderReviewRequestDto();
        dto.setServiceRating(5);
        var dr = new OrderReviewRequestDto.DishRatingDto();
        dr.setDishId(99); // Dish not in order
        dr.setRating(5);
        dto.setDishRatings(List.of(dr));

        // Act
        orderReviewService.submitReview(orderId, userId, dto);

        // Assert
        verify(orderDishReviewRepository, never()).save(any());
    }
}
