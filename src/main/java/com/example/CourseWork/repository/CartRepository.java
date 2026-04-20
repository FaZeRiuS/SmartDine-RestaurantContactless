package com.example.CourseWork.repository;

import com.example.CourseWork.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    /**
     * One round-trip for cart lines + dishes (avoids N+1 when mapping cart to DTO).
     */
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.dish WHERE c.userId = :userId")
    Optional<Cart> findByUserIdWithItemsAndDishes(@Param("userId") String userId);
}
