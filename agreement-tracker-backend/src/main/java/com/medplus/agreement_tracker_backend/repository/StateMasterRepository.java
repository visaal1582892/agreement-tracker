package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.StateMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateMasterRepository extends JpaRepository<StateMaster, Long>, JpaSpecificationExecutor<StateMaster> {

    List<StateMaster> findByIsActiveTrueOrderByStateNameAsc();

    List<StateMaster> findByIdInAndIsActiveTrue(List<Long> ids);

    boolean existsByStateCodeIgnoreCase(String stateCode);

    Optional<StateMaster> findByStateCodeIgnoreCase(String stateCode);

    Optional<StateMaster> findByStateName(String stateName);
}
