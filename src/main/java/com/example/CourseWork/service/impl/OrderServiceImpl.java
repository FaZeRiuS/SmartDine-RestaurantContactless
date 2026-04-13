package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.*;
import com.example.CourseWork.dto.*;
import com.example.CourseWork.mapper.OrderMapper;
import com.example.CourseWork.model.*;
import com.example.CourseWork.repository.*;
import com.example.CourseWork.service.OrderService;
import com.example.CourseWork.service.PaymentService;
import com.example.CourseWork.service.SseService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final CartRepository cartRepository;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final SseService sseService;
    private final OrderServiceReviewRepository orderServiceReviewRepository;
    private final OrderDishReviewRepository orderDishReviewRepository;
    private final com.example.CourseWork.service.PushNotificationService pushNotificationService;

    private void notifyUserOfUpdate(String userId, OrderResponseDto order) {
        if (userId != null) {
            sseService.sendOrderUpdate(userId, order);
            
            // If the order reaches a terminal state, send a robust reload signal
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
                sseService.sendUserNotification(userId, "[RELOAD] Order " + order.getId() + " is finalized (" + order.getStatus() + ")");
            }
        }
    }

    @Override
    public OrderResponseDto createOrder(String userId, OrderRequestDto dto, Integer tableNumber) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTableNumber(tableNumber);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);

        for (OrderItemDto itemDto : dto.getItems()) {
            Dish dish = dishRepository.findById(itemDto.getDishId())
                    .orElseThrow(() -> new RuntimeException("Dish not found: " + itemDto.getDishId()));

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

        float total = order.getItems().stream()
                .map(item -> item.getDish().getPrice() * item.getQuantity())
                .reduce(0f, (a, b) -> a + b);

        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(saved);
        
        // Notify Staff
        pushNotificationService.sendNotificationToRole("ROLE_WAITER", 
            "{\"title\": \"Нове замовлення!\", \"body\": \"Стіл №" + tableNumber + "\", \"url\": \"/staff/orders\"}");
        sseService.sendStaffNotification("New order created: " + response.getId());
            
        return response;
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getAllOrders() {
        List<OrderResponseDto> dtos = orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(orderMapper::toResponseDto).collect(Collectors.toList());
        enrichOrdersWithReviews(dtos);
        return dtos;
    }

    @Transactional
    @Override
    public OrderResponseDto getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security check: Order owner or Staff/Admin only
        com.example.CourseWork.model.KeycloakUser currentUser = com.example.CourseWork.util.KeycloakUtil.getCurrentUser();
        boolean isOwner = order.getUserId().equals(currentUser.getId());
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR") || 
                               a.getAuthority().equals("ROLE_CHEF") || 
                               a.getAuthority().equals("ROLE_WAITER"));

        if (!isOwner && !isStaff) {
            throw new RuntimeException("Access denied");
        }

        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getNewOrders() {
        List<Order> newOrders = orderRepository.findAllByStatusOrderByCreatedAtDesc(OrderStatus.NEW);
        if (newOrders.isEmpty()) {
            throw new RuntimeException("New orders not found");
        }
        return newOrders.stream()
                .map(orderMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemQuantity(Integer orderId, String userId, Integer itemId, Integer quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or access denied"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        if (quantity < item.getQuantity()) {
            if (order.getStatus() != OrderStatus.NEW || PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
                throw new RuntimeException("Cannot decrease quantity for an order that is already processing or paid.");
            }
        }

        if (quantity <= 0) {
            order.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }
        
        float total = order.getItems().stream()
                .map(i -> i.getDish().getPrice() * i.getQuantity())
                .reduce(0f, (a, b) -> a + b);
        order.setTotalPrice(total);

        orderRepository.save(order);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            sseService.sendStaffNotification("[RELOAD] Order cancelled: " + order.getId());
        } else {
            sseService.sendStaffNotification("Order updated: " + order.getId());
        }
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderItemSpecialRequest(Integer orderId, String userId, Integer itemId, String specialRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or access denied"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        
        if (order.getStatus() != OrderStatus.NEW || PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Cannot modify items for an order that is already processing or paid.");
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        item.setSpecialRequest(specialRequest);
        
        orderRepository.save(order);
        sseService.sendStaffNotification("Order updated: " + order.getId());
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public OrderResponseDto removeOrderItem(Integer orderId, String userId, Integer itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found or access denied"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        
        if (order.getStatus() != OrderStatus.NEW || PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Cannot modify items for an order that is already processing or paid.");
        }

        order.getItems().removeIf(i -> i.getId().equals(itemId));
        
        float total = order.getItems().stream()
                .map(i -> i.getDish().getPrice() * i.getQuantity())
                .reduce(0f, (a, b) -> a + b);
        order.setTotalPrice(total);
        
        if (order.getItems().isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            sseService.sendStaffNotification("[RELOAD] Order cancelled: " + order.getId());
        } else {
            sseService.sendStaffNotification("Order updated: " + order.getId());
        }
        return orderMapper.toResponseDto(order);
    }

    @Transactional
    @Override
    public List<OrderResponseDto> getActiveOrders() {
        List<OrderStatus> activeStatuses = List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
        List<OrderResponseDto> dtos = orderRepository.findByStatusInOrderByCreatedAtDesc(activeStatuses)
                .stream().map(orderMapper::toResponseDto).collect(Collectors.toList());
        enrichOrdersWithReviews(dtos);
        return dtos;
    }

    @Transactional
    public OrderResponseDto confirmOrderFromCart(String userId, Integer tableNumber) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTableNumber(tableNumber);
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setDish(cartItem.getDish());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setSpecialRequest(cartItem.getSpecialRequest());
                    orderItem.setOrder(order);
                    return orderItem;
                }).collect(Collectors.toList());

        float total = orderItems.stream()
                .map(item -> item.getDish().getPrice() * item.getQuantity())
                .reduce(0f, (a, b) -> a + b);

        order.setTotalPrice(total);
        order.setItems(orderItems);

        orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        notifyUserOfUpdate(order.getUserId(), response);

        // Notify Staff
        pushNotificationService.sendNotificationToRole("ROLE_WAITER", 
            "{\"title\": \"Нове замовлення!\", \"body\": \"Стіл №" + tableNumber + "\", \"url\": \"/staff/orders\"}");
        sseService.sendStaffNotification("New order confirmed from cart: " + response.getId());

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto updateOrderStatus(Integer Id, OrderStatus newStatus) {
        // Security check (already checked at controller level, but here for defense-in-depth)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR") || 
                               a.getAuthority().equals("ROLE_CHEF") || 
                               a.getAuthority().equals("ROLE_WAITER"));
        
        if (!isStaff) {
            throw new RuntimeException("Only staff can update order status");
        }

        Order order = orderRepository.findById(Id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (newStatus == OrderStatus.READY && order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(newStatus);
        }

        Order updatedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(updatedOrder);
        notifyUserOfUpdate(order.getUserId(), response);

        // Notify Customer
        String statusText = switch(newStatus) {
            case NEW -> "Замовлення прийнято";
            case PREPARING -> "Замовлення готується";
            case READY -> "Замовлення готове! Смачного!";
            case COMPLETED -> "Замовлення завершено. Дякуємо!";
            case CANCELLED -> "Замовлення скасовано";
        };
        pushNotificationService.sendNotificationToUser(order.getUserId(), 
            "{\"title\": \"SmartDine\", \"body\": \"" + statusText + "\", \"url\": \"/orders\"}");

        // If READY, notify Waiter as well
        if (newStatus == OrderStatus.READY) {
            pushNotificationService.sendNotificationToRole("ROLE_WAITER", 
                "{\"title\": \"Замовлення готове!\", \"body\": \"Стіл №" + order.getTableNumber() + "\", \"url\": \"/staff/orders\"}");
        }

        if (updatedOrder.getStatus() == OrderStatus.COMPLETED || updatedOrder.getStatus() == OrderStatus.CANCELLED) {
            sseService.sendStaffNotification("[RELOAD] Order status updated: " + order.getId());
        } else {
            sseService.sendStaffNotification("Order status updated: " + order.getId());
        }

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto addItemsToOrder(Integer orderId, String userId, OrderRequestDto dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot add items to a completed or cancelled order");
        }

        if (order.getStatus() == OrderStatus.READY) {
            order.setStatus(OrderStatus.PREPARING);
        }

        for (OrderItemDto itemDto : dto.getItems()) {
            Dish dish = dishRepository.findById(itemDto.getDishId())
                    .orElseThrow(() -> new RuntimeException("Dish not found: " + itemDto.getDishId()));

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

        float total = order.getItems().stream()
                .map(item -> item.getDish().getPrice() * item.getQuantity())
                .reduce(0f, (a, b) -> a + b);

        order.setTotalPrice(total);

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(savedOrder);
        notifyUserOfUpdate(order.getUserId(), response);
        sseService.sendStaffNotification("Items added to order: " + order.getId());
        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto payOrder(Integer id, String userId, String paymentMethodId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Order is already paid");
        }

        try {
            // For LiqPay, we use the order ID as the unique payment reference
            boolean success = paymentService.processPayment(order.getTotalPrice(), String.valueOf(id));
            if (success) {
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                if (order.getStatus() == OrderStatus.READY) {
                    order.setStatus(OrderStatus.COMPLETED);
                }
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                throw new RuntimeException("Payment failed");
            }
        } catch (Exception e) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new RuntimeException("Payment processing error: " + e.getMessage());
        }

        Order savedOrder = orderRepository.save(order);
        OrderResponseDto response = orderMapper.toResponseDto(savedOrder);
        notifyUserOfUpdate(order.getUserId(), response);
        
        if (savedOrder.getStatus() == OrderStatus.COMPLETED) {
            sseService.sendStaffNotification("[RELOAD] Order paid and completed: " + order.getId());
        } else {
            sseService.sendStaffNotification("Order paid: " + order.getId());
        }
        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto getMyActiveOrder(String userId) {
        // Active orders are those that are not COMPLETED or CANCELLED
        List<OrderStatus> activeStatuses = List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
        OrderResponseDto dto = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> activeStatuses.contains(o.getStatus()))
                .findFirst()
                .map(orderMapper::toResponseDto)
                .orElse(null);
        if (dto == null) return null;
        enrichOrdersWithReviews(List.of(dto));
        return dto;
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

    @Transactional
    @Override
    public OrderResponseDto callWaiter(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot call waiter for a completed or cancelled order");
        }

        order.setNeedsWaiter(true);
        orderRepository.save(order);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        
        // Notify Staff via SSE
        sseService.sendStaffNotification("Waiter called for Table #" + order.getTableNumber());
        
        // Notify Staff via Push
        pushNotificationService.sendNotificationToRole("ROLE_WAITER", 
            "{\"title\": \"Потрібен офіціант!\", \"body\": \"Стіл №" + order.getTableNumber() + "\", \"url\": \"/staff/orders\"}");

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDto dismissWaiterCall(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setNeedsWaiter(false);
        orderRepository.save(order);

        OrderResponseDto response = orderMapper.toResponseDto(order);
        
        // Notify everyone that the call is dismissed (to update UI)
        sseService.sendStaffNotification("Waiter call dismissed for Table #" + order.getTableNumber());
        notifyUserOfUpdate(order.getUserId(), response);

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderResponseDto> getOrderHistory(String userId) {
        List<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<OrderResponseDto> dtos = orders.stream().map(orderMapper::toResponseDto).toList();
        enrichOrdersWithReviews(dtos);
        return dtos;
    }
}
