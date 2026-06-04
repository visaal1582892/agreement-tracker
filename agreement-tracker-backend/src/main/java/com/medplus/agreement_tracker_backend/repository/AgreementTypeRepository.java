package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementTypeRepository extends JpaRepository<AgreementType, Long>, JpaSpecificationExecutor<AgreementType> {

    List<AgreementType> findByIsActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}
