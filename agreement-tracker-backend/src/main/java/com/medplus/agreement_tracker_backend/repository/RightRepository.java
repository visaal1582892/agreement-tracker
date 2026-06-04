package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Right;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RightRepository extends JpaRepository<Right, Long>, JpaSpecificationExecutor<Right> {

    Optional<Right> findByCode(String code);

    List<Right> findByIsActiveTrue();

    List<Right> findByModule(String module);
}
