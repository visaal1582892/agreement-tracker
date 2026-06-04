package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.VendorMaster;
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
public interface VendorMasterRepository extends JpaRepository<VendorMaster, Long>, JpaSpecificationExecutor<VendorMaster> {

    Optional<VendorMaster> findByVendorCode(String vendorCode);

    List<VendorMaster> findByIsActiveTrue();

    @Query("SELECT v FROM VendorMaster v WHERE v.isActive = true AND (LOWER(v.vendorName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<VendorMaster> searchVendors(@Param("search") String search);

    List<VendorMaster> findByIdIn(List<Long> ids);

    Page<VendorMaster> findByIsActiveTrue(Pageable pageable);
}
