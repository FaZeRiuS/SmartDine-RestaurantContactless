package com.example.CourseWork.repository;

import com.example.CourseWork.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByUserId(String userId);
    List<PushSubscription> findAllByRolesContaining(String role);
}
