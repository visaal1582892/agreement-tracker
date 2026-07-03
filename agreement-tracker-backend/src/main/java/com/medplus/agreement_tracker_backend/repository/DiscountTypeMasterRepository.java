package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.DiscountTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountTypeMasterRepository extends JpaRepository<DiscountTypeMaster, Long> {

    List<DiscountTypeMaster> findByIsActiveTrueOrderByDiscountNameAsc();

    Optional<DiscountTypeMaster> findByDiscountNameIgnoreCaseAndIsActiveTrue(String discountName);

    Optional<DiscountTypeMaster> findByDiscountCodeIgnoreCaseAndIsActiveTrue(String discountCode);
}
