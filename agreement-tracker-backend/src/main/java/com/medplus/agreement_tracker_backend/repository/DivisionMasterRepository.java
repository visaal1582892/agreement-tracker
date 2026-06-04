package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.DivisionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivisionMasterRepository extends JpaRepository<DivisionMaster, Long>, JpaSpecificationExecutor<DivisionMaster> {

    List<DivisionMaster> findByManufacturerIdAndIsActiveTrue(Long manufacturerId);

    List<DivisionMaster> findByIsActiveTrue();
}
