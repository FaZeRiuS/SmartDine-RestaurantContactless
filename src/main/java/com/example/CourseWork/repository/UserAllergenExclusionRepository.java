package com.example.CourseWork.repository;

import com.example.CourseWork.model.UserAllergenExclusion;
import com.example.CourseWork.model.UserAllergenExclusionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAllergenExclusionRepository extends JpaRepository<UserAllergenExclusion, UserAllergenExclusionId> {

    @Query("select e.id.allergen from UserAllergenExclusion e where e.id.userId = :userId")
    List<String> findAllAllergensByUserId(@Param("userId") String userId);

    @Modifying
    @Query("delete from UserAllergenExclusion e where e.id.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}

