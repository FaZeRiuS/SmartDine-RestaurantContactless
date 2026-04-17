package com.example.CourseWork.repository;

import com.example.CourseWork.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Integer> {
    List<Dish> findByIsAvailableTrue();

    @Query(value = """
        WITH RecentDishes AS (
            SELECT oi.dish_id
            FROM orders o
            JOIN order_item oi ON o.id = oi.order_id
            WHERE o.user_id = :userId
            ORDER BY o.created_at DESC
            LIMIT 10
        ),
        UserTags AS (
            SELECT dt.tag as tag_name, count(dt.tag) as freq
            FROM RecentDishes rd
            JOIN dish_tags dt ON rd.dish_id = dt.dish_id
            GROUP BY dt.tag
        ),
        DishPopularity AS (
            SELECT dish_id, count(id) as order_count
            FROM order_item
            GROUP BY dish_id
        ),
        ScoredDishes AS (
            SELECT d.id as dish_id,
                (COALESCE(SUM(ut.freq) * 2, 0) + COALESCE((dp.order_count * 5.0 / mp.max_orders), 0)) as match_score
            FROM dish d
            LEFT JOIN dish_tags dt ON d.id = dt.dish_id
            LEFT JOIN UserTags ut ON dt.tag = ut.tag_name
            LEFT JOIN DishPopularity dp ON d.id = dp.dish_id
            CROSS JOIN (SELECT COALESCE(max(order_count), 1) as max_orders FROM DishPopularity) mp
            WHERE d.is_available = true
            GROUP BY d.id, dp.order_count, mp.max_orders
        ),
        RankedDishes AS (
            SELECT d.id as dish_id,
                   sd.match_score,
                   ROW_NUMBER() OVER (PARTITION BY md.menu_id ORDER BY sd.match_score DESC) as rn
            FROM dish d
            JOIN dish_menus md ON d.id = md.dish_id
            JOIN menu m ON md.menu_id = m.id
            JOIN ScoredDishes sd ON d.id = sd.dish_id
            WHERE m.id > 0
        ),
        FinalScored AS (
            SELECT d.*, rd.match_score,
                   ROW_NUMBER() OVER (PARTITION BY d.id ORDER BY rd.match_score DESC, rd.rn ASC) as dish_rn
            FROM dish d
            JOIN RankedDishes rd ON d.id = rd.dish_id
            WHERE rd.rn <= 2
        )
        SELECT fs.id, fs.name, fs.description, fs.price, fs.image_url, fs.is_available
        FROM FinalScored fs
        WHERE fs.dish_rn = 1
        ORDER BY fs.match_score DESC
        LIMIT 6
        """, nativeQuery = true)
    List<Dish> findRecommendedDishes(@Param("userId") String userId);

    @Query(value = """
        SELECT d.id, COUNT(oi2.id) as cnt
        FROM dish d
        JOIN order_item oi2 ON d.id = oi2.dish_id
        JOIN order_item oi1 ON oi1.order_id = oi2.order_id
        WHERE oi1.dish_id = :baseDishId
          AND d.id != :baseDishId
          AND d.is_available = true
          AND NOT EXISTS (
              SELECT 1
              FROM dish_menus m1
              JOIN dish_menus m2 ON m1.menu_id = m2.menu_id
              WHERE m1.dish_id = :baseDishId AND m2.dish_id = d.id
          )
        GROUP BY d.id
        ORDER BY COUNT(oi2.id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSmartComboCandidatesForDish(@Param("baseDishId") Integer baseDishId, @Param("limit") int limit);

    @Query(value = """
        SELECT d.id, COUNT(oi.id) as cnt
        FROM dish d
        LEFT JOIN order_item oi ON d.id = oi.dish_id
        WHERE d.is_available = true
          AND d.id != :baseDishId
          AND NOT EXISTS (
              SELECT 1
              FROM dish_menus m1
              JOIN dish_menus m2 ON m1.menu_id = m2.menu_id
              WHERE m1.dish_id = :baseDishId AND m2.dish_id = d.id
          )
        GROUP BY d.id
        ORDER BY COUNT(oi.id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularComboFallbackCandidates(@Param("baseDishId") Integer baseDishId, @Param("limit") int limit);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM dish_menus m1
        JOIN dish_menus m2 ON m1.menu_id = m2.menu_id
        WHERE m1.dish_id = :dishId AND m2.dish_id IN (:existingIds) AND m1.dish_id != m2.dish_id
        """, nativeQuery = true)
    boolean checkIfSharesMenu(@Param("dishId") Integer dishId, @Param("existingIds") List<Integer> existingIds);
}
