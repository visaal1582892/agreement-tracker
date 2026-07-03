package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.ConsumerPriceOffCampaign;
import com.medplus.agreement_tracker_backend.enums.PriceOffApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumerPriceOffCampaignRepository extends JpaRepository<ConsumerPriceOffCampaign, Long>,
        JpaSpecificationExecutor<ConsumerPriceOffCampaign> {

    Page<ConsumerPriceOffCampaign> findByApprovalStatus(
            PriceOffApprovalStatus approvalStatus,
            Pageable pageable);

    @Query("""
            SELECT c FROM ConsumerPriceOffCampaign c
            JOIN FETCH c.product
            WHERE c.id = :id
            """)
    Optional<ConsumerPriceOffCampaign> findByIdWithProduct(@Param("id") Long id);

    List<ConsumerPriceOffCampaign> findByIdIn(List<Long> ids);
}
