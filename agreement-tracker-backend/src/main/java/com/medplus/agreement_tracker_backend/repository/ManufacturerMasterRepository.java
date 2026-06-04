package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.ManufacturerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturerMasterRepository extends JpaRepository<ManufacturerMaster, Long>, JpaSpecificationExecutor<ManufacturerMaster> {

    List<ManufacturerMaster> findByIsActiveTrue();
}
