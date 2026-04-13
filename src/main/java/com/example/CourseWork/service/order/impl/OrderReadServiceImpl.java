package com.example.CourseWork.service.order.impl;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.dto.OrderResponseDto;
import com.example.CourseWork.exception.ForbiddenException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderServiceReviewRepository;
import com.example.CourseWork.service.order.OrderReadService;
import com.example.CourseWork.service.order.component.OrderAccessPolicy;
import com.example.CourseWork.service.order.component.OrderNotifier;
import com.example.CourseWork.service.security.CurrentUserIdentity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderReadServiceImpl implements OrderReadService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderAccessPolicy orderAccessPolicy;
    private final OrderNotifier orderNotifier;
    private final OrderServiceReviewRepository orderServiceReviewRepository;
    private final OrderDishReviewRepository orderDishReviewRepository;
    private final CurrentUserIdentity currentUserIdentity;

    public OrderReadServiceImpl(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            OrderAccessPolicy orderAccessPolicy,
            OrderNotifier orderNotifier,
            OrderServiceReviewRepository orderServiceReviewRepository,
            OrderDishReviewRepository orderDishReviewRepository,
            CurrentUserIdentity currentUserIdentity
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderAccessPolicy = orderAccessPolicy;
        this.orderNotifier = orderNotifier;
        this.orderServiceReviewRepository = orderServiceReviewRepository;
        this.orderDishReviewRepository = orderDishReviewRepository;
        this.currentUserIdentity = currentUserIdentity;
    }

    @Transactional
    @Override
    public Page<OrderResponseDto> getAllOrders(Pageable pageable) {
        Page<OrderResponseDto> page = orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(orderMapper::toResponseDto);
        enrichOrdersWithReviews(page.getContent());
        return page;
    }

    @Transactional
    @Override
    public OrderResponseDto getOrderById(Integer id) {
        @SuppressWarnings("null")
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        boolean isOwner = order.getUserId().equals(currentUserIdentity.currentUserId());
        boolean isStaff = orderAccessPolicy.isStaff();
        if (!isOwner && !isStaff) {
            throw new ForbiddenException(ErrorMessages.ACCESS_DENIED);
        }

        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getNewOrders() {
        List<Order> newOrders = orderRepository.findAllByStatusOrderByCreatedAtDesc(OrderStatus.NEW);
        return newOrders.stream().map(orderMapper::toResponseDto).toList();
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getActiveOrders() {
        List<OrderStatus> activeStatuses = List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
        List<OrderResponseDto> dtos = orderRepository.findByStatusInOrderByCreatedAtDesc(activeStatuses)
                .stream().map(orderMapper::toResponseDto).toList();
        enrichOrdersWithReviews(dtos);
        return dtos;
    }

    @Transactional
    @Override
    public Optional<OrderResponseDto> getMyActiveOrder(String userId) {
        List<OrderStatus> activeStatuses = List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
        Optional<OrderResponseDto> dto = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> activeStatuses.contains(o.getStatus()))
                .findFirst()
                .map(orderMapper::toResponseDto)
                ;
        dto.ifPresent(d -> enrichOrdersWithReviews(List.of(d)));
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponseDto> getOrderHistory(String userId, Pageable pageable) {
        Page<OrderResponseDto> page = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toResponseDto);
        enrichOrdersWithReviews(page.getContent());
        return page;
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        if (!orderAccessPolicy.isStaff()) {
            throw new ForbiddenException(ErrorMessages.ONLY_STAFF_CAN_UPDATE_ORDER_STATUS);
        }

        @SuppressWarnings("null")
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        if (newStatus == OrderStatus.READY && order.getPaymentStatus() != null
                && order.getPaymentStatus().name().equalsIgnoreCase("SUCCESS")) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(newStatus);
        }

        Order updatedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(updatedOrder);
        orderNotifier.notifyStatusChange(order.getUserId(), response, newStatus);

        return response;
    }

    private void enrichOrdersWithReviews(List<OrderResponseDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        List<Integer> orderIds = dtos.stream().map(OrderResponseDto::getId).toList();

        Map<Integer, Integer> ratingByOrderId = new HashMap<>();
        Map<Integer, String> commentByOrderId = new HashMap<>();
        orderServiceReviewRepository.findByOrderIdIn(orderIds).forEach(r -> {
            if (r.getOrder() != null && r.getOrder().getId() != null) {
                ratingByOrderId.put(r.getOrder().getId(), r.getRating());
                commentByOrderId.put(r.getOrder().getId(), r.getComment());
            }
        });

        Map<Integer, Map<Integer, Integer>> dishRatingsByOrderId = new HashMap<>();
        orderDishReviewRepository.findAllByOrderIdIn(orderIds).forEach(r -> {
            Integer oid = r.getOrder() != null ? r.getOrder().getId() : null;
            Integer did = r.getDish() != null ? r.getDish().getId() : null;
            if (oid == null || did == null) return;
            dishRatingsByOrderId.computeIfAbsent(oid, k -> new HashMap<>()).put(did, r.getRating());
        });

        dtos.forEach(dto -> {
            dto.setServiceRating(ratingByOrderId.get(dto.getId()));
            dto.setServiceComment(commentByOrderId.get(dto.getId()));
            if (dto.getItems() != null) {
                Map<Integer, Integer> m = dishRatingsByOrderId.get(dto.getId());
                if (m != null) {
                    dto.getItems().forEach(i -> i.setRating(m.get(i.getDishId())));
                }
            }
        });
    }
}

