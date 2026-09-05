package com.crm.backend.subscription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {

    @EntityGraph(attributePaths = "features")
    Optional<SubscriptionPlan> findByCodeAndActiveTrue(
            SubscriptionPlanCode code
    );

    @EntityGraph(attributePaths = "features")
    List<SubscriptionPlan> findByActiveTrueOrderByDisplayOrderAsc();
}
