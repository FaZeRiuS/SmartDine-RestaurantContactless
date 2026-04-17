package com.example.CourseWork.service.impl;

import com.example.CourseWork.dto.OrderReviewRequestDto;
import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.model.Dish;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.model.OrderDishReview;
import com.example.CourseWork.model.OrderServiceReview;
import com.example.CourseWork.repository.DishRepository;
import com.example.CourseWork.repository.OrderDishReviewRepository;
import com.example.CourseWork.repository.OrderRepository;
import com.example.CourseWork.repository.OrderServiceReviewRepository;
import com.example.CourseWork.service.OrderReviewService;
import com.example.CourseWork.service.order.component.OrderPaymentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderReviewServiceImpl implements OrderReviewService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final OrderServiceReviewRepository orderServiceReviewRepository;
    private final OrderDishReviewRepository orderDishReviewRepository;
    private final OrderPaymentPolicy orderPaymentPolicy;

    @Override
    @Transactional
    public void submitReview(Integer orderId, UUID userId, OrderReviewRequestDto dto) {
        if (orderId == null) throw new BadRequestException(ErrorMessages.ORDER_ID_REQUIRED);
        if (userId == null) throw new BadRequestException(ErrorMessages.USER_ID_REQUIRED);
        if (dto == null) throw new BadRequestException(ErrorMessages.REVIEW_BODY_REQUIRED);

        Order order = orderRepository.findByIdWithItemsAndDishes(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.ORDER_NOT_FOUND));

        orderPaymentPolicy.assertOwner(order, userId.toString());
        orderPaymentPolicy.assertReviewable(order);

        Integer serviceRating = dto.getServiceRating();
        if (serviceRating == null || serviceRating < 1 || serviceRating > 5) {
            throw new BadRequestException(ErrorMessages.INVALID_SERVICE_RATING);
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
        orderDishReviewRepository.flush();

        Set<Integer> allowedDishIds = new HashSet<>();
        if (order.getItems() != null) {
            order.getItems().forEach(i -> {
                if (i.getDish() != null && i.getDish().getId() != null) {
                    allowedDishIds.add(i.getDish().getId());
                }
            });
        }

        // Last rating wins if the client sends duplicate dish IDs
        Map<Integer, Integer> ratingByDishId = new LinkedHashMap<>();
        if (dto.getDishRatings() != null) {
            for (OrderReviewRequestDto.DishRatingDto dr : dto.getDishRatings()) {
                if (dr == null) continue;
                Integer dishId = dr.getDishId();
                Integer rating = dr.getRating();
                if (dishId == null || rating == null) continue;
                if (rating < 1 || rating > 5) {
                    throw new BadRequestException(ErrorMessages.INVALID_DISH_RATING);
                }
                if (!allowedDishIds.contains(dishId)) {
                    continue;
                }
                ratingByDishId.put(dishId, rating);
            }
        }

        for (Map.Entry<Integer, Integer> e : ratingByDishId.entrySet()) {
            Integer dishId = e.getKey();
            Integer rating = e.getValue();
            if (dishId == null || rating == null) {
                continue;
            }
            Dish dish = dishRepository.findById(dishId).orElse(null);
            if (dish == null) {
                continue;
            }

            OrderDishReview dishReview = new OrderDishReview();
            dishReview.setOrder(order);
            dishReview.setDish(dish);
            dishReview.setUserId(userId);
            dishReview.setRating(rating);
            orderDishReviewRepository.save(dishReview);
        }
    }
}

