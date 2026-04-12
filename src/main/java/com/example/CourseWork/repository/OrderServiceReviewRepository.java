package com.example.CourseWork.repository;

import com.example.CourseWork.model.OrderServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderServiceReviewRepository extends JpaRepository<OrderServiceReview, Long> {
    Optional<OrderServiceReview> findByOrderId(Integer orderId);
    List<OrderServiceReview> findByOrderIdIn(List<Integer> orderIds);
}

