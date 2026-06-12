package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.DivisionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivisionMasterRepository extends JpaRepository<DivisionMaster, Long>, JpaSpecificationExecutor<DivisionMaster> {

    @Query("""
            SELECT d FROM DivisionMaster d
            JOIN FETCH d.manufacturer m
            WHERE m.id = :manufacturerId AND d.isActive = true
            ORDER BY d.divisionName ASC
            """)
    List<DivisionMaster> findActiveByManufacturerId(@Param("manufacturerId") Long manufacturerId);

    @Query("""
            SELECT d FROM DivisionMaster d
            JOIN FETCH d.manufacturer m
            WHERE d.isActive = true
            ORDER BY d.divisionName ASC
            """)
    List<DivisionMaster> findAllActiveWithManufacturer();
}
