package com.example.CourseWork.service.impl;

import com.example.CourseWork.addition.OrderStatus;
import com.example.CourseWork.addition.PaymentStatus;
import com.example.CourseWork.dto.OrderReviewRequestDto;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderDishReview;
import com.example.CourseWork.model.OrderServiceReview;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderServiceReviewRepository;
import com.example.CourseWork.service.OrderReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderReviewServiceImpl implements OrderReviewService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final OrderServiceReviewRepository orderServiceReviewRepository;
    private final OrderDishReviewRepository orderDishReviewRepository;

    @Override
    @Transactional
    public void submitReview(Integer orderId, UUID userId, OrderReviewRequestDto dto) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (dto == null) throw new IllegalArgumentException("review body is required");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getUserId() == null || !order.getUserId().equals(userId.toString())) {
            throw new RuntimeException("Unauthorized: Order does not belong to the user");
        }
        if (!PaymentStatus.SUCCESS.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order is not paid");
        }
        if (!(OrderStatus.READY.equals(order.getStatus()) || OrderStatus.COMPLETED.equals(order.getStatus()))) {
            throw new RuntimeException("Order is not ready for review");
        }

        Integer serviceRating = dto.getServiceRating();
        if (serviceRating == null || serviceRating < 1 || serviceRating > 5) {
            throw new RuntimeException("Invalid service rating");
        }

        OrderServiceReview serviceReview = orderServiceReviewRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    OrderServiceReview r = new OrderServiceReview();
                    r.setOrder(order);
                    r.setUserId(userId);
                    return r;
                });
        serviceReview.setRating(serviceRating);
        String comment = dto.getComment();
        if (comment != null) {
            comment = comment.trim();
            if (comment.isEmpty()) comment = null;
            if (comment != null && comment.length() > 1000) {
                comment = comment.substring(0, 1000);
            }
        }
        serviceReview.setComment(comment);
        orderServiceReviewRepository.save(serviceReview);

        // Replace dish reviews for this order atomically
        orderDishReviewRepository.deleteAllByOrderId(orderId);

        Set<Integer> allowedDishIds = new HashSet<>();
        if (order.getItems() != null) {
            order.getItems().forEach(i -> {
                if (i.getDish() != null && i.getDish().getId() != null) {
                    allowedDishIds.add(i.getDish().getId());
                }
            });
        }

        if (dto.getDishRatings() != null) {
            for (OrderReviewRequestDto.DishRatingDto dr : dto.getDishRatings()) {
                if (dr == null) continue;
                Integer dishId = dr.getDishId();
                Integer rating = dr.getRating();
                if (dishId == null || rating == null) continue;
                if (rating < 1 || rating > 5) {
                    throw new RuntimeException("Invalid dish rating");
                }
                if (!allowedDishIds.contains(dishId)) {
                    continue;
                }

                Dish dish = dishRepository.findById(dishId)
                        .orElseThrow(() -> new RuntimeException("Dish not found: " + dishId));

                OrderDishReview dishReview = new OrderDishReview();
                dishReview.setOrder(order);
                dishReview.setDish(dish);
                dishReview.setUserId(userId);
                dishReview.setRating(rating);
                orderDishReviewRepository.save(dishReview);
            }
        }
    }
}

