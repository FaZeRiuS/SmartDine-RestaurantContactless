package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderItemDto;
import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
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
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.util.SpecialRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            OrderNotifier orderNotifier) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.orderMapper = orderMapper;
        this.orderTotalCalculator = orderTotalCalculator;
        this.orderNotifier = orderNotifier;
    }

    @CacheEvict(cacheNames = "personalizedRecommendations", key = "#userId")
    @Override
    public OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTableNumber(tableNumber);
        order.setCreatedAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setItems(new ArrayList<>());

        // Batch fetch all dishes to avoid N+1 query pattern (Issue 13)
        java.util.List<Integer> dishIds = dto.getItems().stream()
                .map(OrderItemDto::getDishId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        java.util.Map<Integer, Dish> dishMap = java.util.Collections.emptyMap();
        if (!dishIds.isEmpty()) {
            dishMap = dishRepository.findAllById(dishIds).stream()
                    .collect(Collectors.toMap(Dish::getId, Function.identity()));
        }

        for (OrderItemDto itemDto : dto.getItems()) {
            Integer dishId = itemDto.getDishId();
            Dish dish = dishMap.get(dishId);
            if (dish == null) {
                throw new NotFoundException(ErrorMessages.DISH_NOT_FOUND);
            }
            if (Boolean.FALSE.equals(dish.getIsAvailable())) {
                throw new BadRequestException(ErrorMessages.DISH_NOT_AVAILABLE);
            }

            final String req = SpecialRequestUtil.normalize(itemDto.getSpecialRequest());
            OrderItem existingItem = order.getItems().stream()
                    .filter(item -> item.getDish().getId().equals(dish.getId()) &&
                            java.util.Objects.equals(SpecialRequestUtil.normalize(item.getSpecialRequest()), req))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + itemDto.getQuantity());
            } else {
                OrderItem item = new OrderItem();
                item.setDish(dish);
                item.setQuantity(itemDto.getQuantity());
                item.setSpecialRequest(req);
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
