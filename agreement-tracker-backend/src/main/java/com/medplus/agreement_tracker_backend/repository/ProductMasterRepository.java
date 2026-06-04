package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.ProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long>, JpaSpecificationExecutor<ProductMaster> {

    List<ProductMaster> findByManufacturerIdAndIsActiveTrue(Long manufacturerId);

    List<ProductMaster> findByDivisionIdAndIsActiveTrue(Long divisionId);

    @Query("""
            SELECT DISTINCT p FROM ProductMaster p
            JOIN VendorProductMapping vpm ON vpm.product.id = p.id
            WHERE vpm.vendor.id IN :vendorIds AND p.isActive = true
            """)
    List<ProductMaster> findByVendorIds(@Param("vendorIds") List<Long> vendorIds);

    @Query("""
            SELECT DISTINCT p FROM ProductMaster p
            JOIN VendorProductMapping vpm ON vpm.product.id = p.id
            WHERE vpm.vendor.id IN :vendorIds
            AND p.manufacturer.id = :manufacturerId
            AND p.isActive = true
            """)
    List<ProductMaster> findByVendorIdsAndManufacturer(@Param("vendorIds") List<Long> vendorIds, @Param("manufacturerId") Long manufacturerId);

    @Query("""
            SELECT DISTINCT p FROM ProductMaster p
            JOIN VendorProductMapping vpm ON vpm.product.id = p.id
            WHERE vpm.vendor.id IN :vendorIds
            AND p.division.id IN :divisionIds
            AND p.isActive = true
            """)
    List<ProductMaster> findByVendorIdsAndDivisions(@Param("vendorIds") List<Long> vendorIds, @Param("divisionIds") List<Long> divisionIds);
}
