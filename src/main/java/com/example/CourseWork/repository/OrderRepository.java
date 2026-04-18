package com.example.CourseWork.repository;

import com.example.CourseWork.model.OrderStatus;
import com.example.CourseWork.model.PaymentStatus;
import com.example.CourseWork.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    /**
     * Loads order lines and dishes in one query (OSIV-safe). Avoids lazy-load failures when a dish row was removed
     * but order lines still reference it.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.dish WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndDishes(@Param("id") Integer id);

    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = { "items", "items.dish" })
    List<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses);

    List<Order> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Eager-fetch order lines for customer history (OSIV is off — collections must be initialized in one query).
     */
    @Query(
            value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.dish "
                    + "WHERE o.userId = :userId AND o.status IN :statuses",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status IN :statuses")
    Page<Order> findPageWithItemsAndDishesForUserAndStatuses(
            @Param("userId") String userId,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus, Pageable pageable);

    Page<Order> findByStatusAndPaymentStatusOrderByCreatedAtDesc(
            OrderStatus status, PaymentStatus paymentStatus, Pageable pageable);

    interface TopDishView {
        String getName();
        Long getQuantity();
    }

    interface HourCountView {
        Integer getOrderHour();
        Long getCount();
    }

    @Query(value = """
        SELECT COALESCE(SUM(o.total_price), 0)
        FROM orders o
        WHERE o.payment_status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        """, nativeQuery = true)
    BigDecimal sumRevenue(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT COUNT(*)
        FROM orders o
        WHERE o.payment_status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        """, nativeQuery = true)
    Long countSuccessfulOrders(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT COUNT(*)
        FROM orders o
        WHERE o.payment_status = 'SUCCESS'
          AND o.user_id = :userId
        """, nativeQuery = true)
    Long countSuccessfulOrdersByUserId(@Param("userId") String userId);

    @Query(value = """
        SELECT COALESCE(AVG(o.total_price), 0)
        FROM orders o
        WHERE o.payment_status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        """, nativeQuery = true)
    BigDecimal avgCheck(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT d.name AS name, COALESCE(SUM(oi.quantity), 0) AS quantity
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN dish d ON d.id = oi.dish_id
        WHERE o.payment_status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        GROUP BY d.name
        ORDER BY quantity DESC
        LIMIT 5
        """, nativeQuery = true)
    List<TopDishView> findTopDishes(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT CAST(EXTRACT(HOUR FROM o.created_at) AS INT) AS orderHour, COUNT(*) AS count
        FROM orders o
        WHERE o.payment_status = 'SUCCESS'
          AND o.created_at >= :from
          AND o.created_at < :to
        GROUP BY orderHour
        ORDER BY orderHour
        """, nativeQuery = true)
    List<HourCountView> countSuccessfulOrdersByHour(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
