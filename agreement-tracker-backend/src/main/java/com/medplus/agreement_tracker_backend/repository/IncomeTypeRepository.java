package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.IncomeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeTypeRepository extends JpaRepository<IncomeType, Long>, JpaSpecificationExecutor<IncomeType> {

    List<IncomeType> findByIsActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
