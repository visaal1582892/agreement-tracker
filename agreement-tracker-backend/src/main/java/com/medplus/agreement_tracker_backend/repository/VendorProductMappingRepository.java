package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.VendorProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorProductMappingRepository extends JpaRepository<VendorProductMapping, Long> {

    boolean existsByVendorIdAndProductId(Long vendorId, Long productId);
}
