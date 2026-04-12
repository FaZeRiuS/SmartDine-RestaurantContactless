package com.example.CourseWork.repository;

import com.example.CourseWork.model.OrderDishReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDishReviewRepository extends JpaRepository<OrderDishReview, Long> {
    Optional<OrderDishReview> findByOrderIdAndDishId(Integer orderId, Integer dishId);

    void deleteAllByOrderId(Integer orderId);

    List<OrderDishReview> findAllByOrderIdIn(List<Integer> orderIds);

    interface DishRatingAgg {
        Integer getDishId();
        Double getAvgRating();
        Long getRatingsCount();
    }

    @Query(value = """
        SELECT r.dish_id AS dishId,
               AVG(r.rating) AS avgRating,
               COUNT(*) AS ratingsCount
        FROM order_dish_review r
        WHERE r.dish_id IN (:dishIds)
        GROUP BY r.dish_id
        """, nativeQuery = true)
    List<DishRatingAgg> aggregateRatingsForDishIds(@Param("dishIds") List<Integer> dishIds);
}

