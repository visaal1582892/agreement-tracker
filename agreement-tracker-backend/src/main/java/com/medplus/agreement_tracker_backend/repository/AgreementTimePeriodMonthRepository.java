package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriodMonth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgreementTimePeriodMonthRepository extends JpaRepository<AgreementTimePeriodMonth, Long> {

    List<AgreementTimePeriodMonth> findByTimePeriodIdOrderByCalendarYearAscCalendarMonthAsc(Long timePeriodId);
}
