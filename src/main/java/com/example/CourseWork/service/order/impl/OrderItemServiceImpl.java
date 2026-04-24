package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.dto.order.OrderItemDto;
import com.example.CourseWork.dto.order.OrderRequestDto;
import com.example.CourseWork.dto.order.OrderResponseDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Cart;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderItem;
import com.example.CourseWork.repository.CartRepository;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.service.order.OrderItemService;
import com.example.CourseWork.service.order.component.OrderAccessPolicy;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.order.component.OrderTotalCalculator;
import com.example.CourseWork.i18n.NotificationMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final CartRepository cartRepository;
    private final OrderMapper orderMapper;
    private final OrderTotalCalculator orderTotalCalculator;
    private final OrderAccessPolicy orderAccessPolicy;
    private final OrderNotifier orderNotifier;

    private static String normalizeSpecialRequest(String value) {
        if (value == null) return "";
        String v = value.trim();
        return v.isBlank() ? "" : v;
    }

    public OrderItemServiceImpl(
            OrderRepository orderRepository,
            DishRepository dishRepository,
            CartRepository cartRepository,
            OrderMapper orderMapper,
            OrderTotalCalculator orderTotalCalculator,
            OrderAccessPolicy orderAccessPolicy,
            OrderNotifier orderNotifier
    ) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.cartRepository = cartRepository;
        this.orderMapper = orderMapper;
        this.orderTotalCalculator = orderTotalCalculator;
        this.orderAccessPolicy = orderAccessPolicy;
        this.orderNotifier = orderNotifier;
    }

    @Transactional
    @Override
    public OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber) {
        Cart cart = cartRepository.findByUserIdWithItemsAndDishes(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException(ErrorMessages.CART_EMPTY);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTableNumber(tableNumber);
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setTipAmount(new BigDecimal("20.00"));
        order.setTipOptOut(false);

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setDish(cartItem.getDish());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setSpecialRequest(normalizeSpecialRequest(cartItem.getSpecialRequest()));
                    orderItem.setOrder(order);
                    return orderItem;
                }).collect(Collectors.toList());

        order.setTotalPrice(orderTotalCalculator.calculateTotal(orderItems));
        order.setItems(orderItems);

        orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        orderNotifier.notifyUserOfUpdate(order.getUserId(), response);

        orderNotifier.notifyWaitersAboutNewOrder(tableNumber, response.getId());
        orderNotifier.sendStaffNotification(NotificationMessages.staffNewOrderCreated(response.getId()));

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderAccessPolicy.assertOwner(order, userId);
        orderAccessPolicy.assertAddItemsAllowed(order);

        if (order.getStatus() == OrderStatus.READY) {
            order.setStatus(OrderStatus.PREPARING);
        }

        for (OrderItemDto itemDto : dto.getItems()) {
            Integer dishId = itemDto.getDishId();
            @SuppressWarnings("null")
            Dish dish = dishRepository.findById(dishId)
                    .orElseThrow(() -> new NotFoundException(ErrorMessages.DISH_NOT_FOUND));

            final String req = normalizeSpecialRequest(itemDto.getSpecialRequest());
            OrderItem existingItem = order.getItems().stream()
                    .filter(item -> item.getDish().getId().equals(dish.getId()) &&
                            Objects.equals(normalizeSpecialRequest(item.getSpecialRequest()), req))
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

        if (order.getStatus() == OrderStatus.PREPARING) {
            int prepTime = orderTotalCalculator.calculateTotalPreparationTimeMinutes(order.getItems());
            order.setEstimatedReadyTime(OffsetDateTime.now().plusMinutes(prepTime));
        }

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(savedOrder);
        orderNotifier.notifyUserOfUpdate(order.getUserId(), response);
        orderNotifier.sendStaffNotification(NotificationMessages.staffOrderUpdated(order.getId(), false));
        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderAccessPolicy.assertOwner(order, userId);

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_ITEM_NOT_FOUND));

        orderAccessPolicy.assertUpdateItemQuantityAllowed(order, item.getQuantity(), quantity);

        if (quantity <= 0) {
            order.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        orderTotalCalculator.recalculateAndSetTotal(order);

        if (order.getStatus() == OrderStatus.PREPARING) {
            int prepTime = orderTotalCalculator.calculateTotalPreparationTimeMinutes(order.getItems());
            order.setEstimatedReadyTime(OffsetDateTime.now().plusMinutes(prepTime));
        }

        if (order.getItems().isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
        orderNotifier.notifyStaffOrderUpdated(order.getId(), order.getStatus() == OrderStatus.CANCELLED);
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderAccessPolicy.assertOwner(order, userId);
        orderAccessPolicy.assertModifiableItems(order);

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_ITEM_NOT_FOUND));

        item.setSpecialRequest(normalizeSpecialRequest(specialRequest));

        orderRepository.save(order);
        orderNotifier.notifyStaffOrderUpdated(order.getId(), false);
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderAccessPolicy.assertOwner(order, userId);
        orderAccessPolicy.assertModifiableItems(order);

        order.getItems().removeIf(i -> i.getId().equals(itemId));

        orderTotalCalculator.recalculateAndSetTotal(order);

        if (order.getStatus() == OrderStatus.PREPARING) {
            int prepTime = orderTotalCalculator.calculateTotalPreparationTimeMinutes(order.getItems());
            order.setEstimatedReadyTime(OffsetDateTime.now().plusMinutes(prepTime));
        }

        if (order.getItems().isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
        orderNotifier.notifyStaffOrderUpdated(order.getId(), order.getStatus() == OrderStatus.CANCELLED);
        return orderMapper.toResponseDto(order);
    }
}

