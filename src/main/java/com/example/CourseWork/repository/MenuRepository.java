package com.example.CourseWork.repository;

import com.example.CourseWork.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Integer> {
    @Query("select distinct m from Menu m left join fetch m.dishes")
    List<Menu> findAllWithDishes();
}
