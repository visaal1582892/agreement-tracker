package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

import java.util.List;

public interface AgreementTimePeriodResolutionService {

    AgreementTimePeriod resolveOrCreatePeriod(String name, PayoutFrequency frequency, Long userId);

    AgreementTimePeriod canonicalizePeriod(
            AgreementTimePeriod period,
            AgreementVersion version,
            Integer financialYearStartMonthOverride,
            Long userId);

    List<AgreementTimePeriod> resolvePeriodsForSlab(AgreementVersion version, AgreementSlab slab, Long userId);

    List<AgreementTimePeriod> resolvePeriodsForSlab(
            AgreementVersion version,
            AgreementSlab slab,
            Long userId,
            Integer financialYearStartMonthOverride);

    boolean periodWithinContract(AgreementTimePeriod period, AgreementVersion version);
}
