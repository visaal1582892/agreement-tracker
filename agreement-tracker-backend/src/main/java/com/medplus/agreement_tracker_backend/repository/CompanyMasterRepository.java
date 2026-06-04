package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.CompanyMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyMasterRepository extends JpaRepository<CompanyMaster, Long>, JpaSpecificationExecutor<CompanyMaster> {

    boolean existsByCompanyNameIgnoreCase(String companyName);

    List<CompanyMaster> findByIsActiveTrue();

    @Query("SELECT c FROM CompanyMaster c WHERE c.isActive = true AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<CompanyMaster> searchByName(@Param("search") String search);

    Page<CompanyMaster> findByIsActiveTrue(Pageable pageable);
}
