package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgreementTimePeriodRepository extends JpaRepository<AgreementTimePeriod, Long> {

    Optional<AgreementTimePeriod> findByName(String name);

    List<AgreementTimePeriod> findAllByOrderByNameAsc();
}
