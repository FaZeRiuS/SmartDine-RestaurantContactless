package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderItemDto;
import com.example.CourseWork.dto.OrderRequestDto;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderCreateService;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.order.component.OrderTotalCalculator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Service
public class OrderCreateServiceImpl implements OrderCreateService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final OrderMapper orderMapper;
    private final OrderTotalCalculator orderTotalCalculator;
    private final OrderNotifier orderNotifier;

    public OrderCreateServiceImpl(
            OrderRepository orderRepository,
            DishRepository dishRepository,
            OrderMapper orderMapper,
            OrderTotalCalculator orderTotalCalculator,
            OrderNotifier orderNotifier
    ) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.orderMapper = orderMapper;
        this.orderTotalCalculator = orderTotalCalculator;
        this.orderNotifier = orderNotifier;
    }

    @Override
    public OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTableNumber(tableNumber);
        order.setCreatedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setItems(new ArrayList<>());

        for (OrderItemDto itemDto : dto.getItems()) {
            Integer dishId = itemDto.getDishId();
            @SuppressWarnings("null")
            Dish dish = dishRepository.findById(dishId)
                    .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));

            OrderItem existingItem = order.getItems().stream()
                    .filter(item -> item.getDish().getId().equals(dish.getId()) &&
                            java.util.Objects.equals(item.getSpecialRequest(), itemDto.getSpecialRequest()))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + itemDto.getQuantity());
            } else {
                OrderItem item = new OrderItem();
                item.setDish(dish);
                item.setQuantity(itemDto.getQuantity());
                item.setSpecialRequest(itemDto.getSpecialRequest());
                item.setOrder(order);
                order.getItems().add(item);
            }
        }

        orderTotalCalculator.recalculateAndSetTotal(order);

        Order saved = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(saved);

        orderNotifier.notifyWaitersAboutNewOrder(tableNumber, response.getId());
        return response;
    }
}

