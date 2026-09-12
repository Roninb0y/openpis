package com.openpis.plan.repository;

import com.openpis.plan.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Page<Plan> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Plan> findByStatus(Integer status, Pageable pageable);

    Page<Plan> findByNameContainingIgnoreCaseAndStatus(String name, Integer status, Pageable pageable);

    List<Plan> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Integer status, LocalDate start, LocalDate end);
}
