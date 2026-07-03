package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriodMonth;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.service.AgreementTimePeriodResolutionService;
import com.medplus.agreement_tracker_backend.util.DynamicFinancialYearPeriodGenerator;
import com.medplus.agreement_tracker_backend.util.TimePeriodDimensions;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementTimePeriodResolutionServiceImpl implements AgreementTimePeriodResolutionService {

    private final AgreementTimePeriodRepository timePeriodRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public AgreementTimePeriod resolveOrCreatePeriod(String name, PayoutFrequency frequency, Long userId) {
        return resolveOrCreatePeriod(name, frequency, userId, null, LocalDate.now());
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public AgreementTimePeriod resolveOrCreatePeriod(
            String name,
            PayoutFrequency frequency,
            Long userId,
            Integer financialYearStartMonthOverride,
            LocalDate anchorDate) {
        int financialYearStartMonth = DynamicFinancialYearPeriodGenerator.resolveStartMonth(financialYearStartMonthOverride);

        return timePeriodRepository.findByName(name)
                .map(existing -> synchronizePeriodName(existing, name, userId))
                .orElseGet(() -> savePeriod(name, frequency, financialYearStartMonth, anchorDate, userId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public AgreementTimePeriod canonicalizePeriod(
            AgreementTimePeriod period,
            AgreementVersion version,
            Integer financialYearStartMonthOverride,
            Long userId) {
        if (period == null || period.getPeriodFrequency() == null) {
            return period;
        }
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            return period;
        }
        int financialYearStartMonth = DynamicFinancialYearPeriodGenerator.resolveStartMonth(
                financialYearStartMonthOverride != null
                        ? financialYearStartMonthOverride
                        : version.getFinancialYearStartMonth());
        List<String> canonicalNames = DynamicFinancialYearPeriodGenerator.generatePeriodNames(
                period.getPeriodFrequency(),
                version.getStartDate(),
                version.getExpiryDate(),
                financialYearStartMonth);

        List<YearMonth> periodMonths = TimePeriodDimensions.sortedIncludedMonths(period);
        for (String canonicalName : canonicalNames) {
            List<YearMonth> canonicalMonths = TimePeriodDimensions.includedMonthsFromName(
                    canonicalName,
                    period.getPeriodFrequency(),
                    financialYearStartMonth,
                    version.getStartDate());
            if (canonicalMonths.equals(periodMonths)) {
                return resolveOrCreatePeriod(
                        canonicalName,
                        period.getPeriodFrequency(),
                        userId,
                        financialYearStartMonth,
                        version.getStartDate());
            }
        }
        return period;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public List<AgreementTimePeriod> resolvePeriodsForSlab(AgreementVersion version, AgreementSlab slab, Long userId) {
        return resolvePeriodsForSlab(version, slab, userId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public List<AgreementTimePeriod> resolvePeriodsForSlab(
            AgreementVersion version,
            AgreementSlab slab,
            Long userId,
            Integer financialYearStartMonthOverride) {
        if (version.getStartDate() == null || version.getExpiryDate() == null || slab.getPayoutFrequency() == null) {
            return List.of();
        }
        int financialYearStartMonth = DynamicFinancialYearPeriodGenerator.resolveStartMonth(
                financialYearStartMonthOverride != null
                        ? financialYearStartMonthOverride
                        : version.getFinancialYearStartMonth());
        List<String> names = DynamicFinancialYearPeriodGenerator.generatePeriodNames(
                slab.getPayoutFrequency(),
                version.getStartDate(),
                version.getExpiryDate(),
                financialYearStartMonth);

        List<AgreementTimePeriod> periods = new ArrayList<>();
        for (String name : names) {
            periods.add(resolveOrCreatePeriod(
                    name,
                    slab.getPayoutFrequency(),
                    userId,
                    financialYearStartMonth,
                    version.getStartDate()));
        }
        periods.sort(TimePeriodDimensions.chronologicalComparator());
        return periods.stream()
                .filter(period -> periodWithinContract(period, version))
                .toList();
    }

    @Override
    public boolean periodWithinContract(AgreementTimePeriod period, AgreementVersion version) {
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            return true;
        }
        if (period.getPeriodFrequency() == null
                || period.getIncludedMonths() == null
                || period.getIncludedMonths().isEmpty()) {
            return true;
        }

        YearMonth periodStart = TimePeriodDimensions.periodStart(period);
        YearMonth periodEnd = TimePeriodDimensions.periodEnd(period);
        YearMonth contractStart = YearMonth.from(version.getStartDate());
        YearMonth contractEnd = YearMonth.from(version.getExpiryDate());

        return !periodEnd.isBefore(contractStart) && !periodStart.isAfter(contractEnd);
    }

    private AgreementTimePeriod synchronizePeriodName(
            AgreementTimePeriod period,
            String canonicalName,
            Long userId) {
        if (canonicalName.equals(period.getName())) {
            return period;
        }
        return timePeriodRepository.findByName(canonicalName)
                .orElseGet(() -> {
                    period.setName(canonicalName);
                    period.setUpdatedByUserId(userId);
                    return timePeriodRepository.save(period);
                });
    }

    private AgreementTimePeriod savePeriod(
            String name,
            PayoutFrequency frequency,
            int financialYearStartMonth,
            LocalDate anchorDate,
            Long userId) {
        AgreementTimePeriod period = AgreementTimePeriod.builder()
                .name(name)
                .periodFrequency(frequency)
                .build();
        populateIncludedMonths(period, name, frequency, financialYearStartMonth, anchorDate);
        period.setCreatedByUserId(userId);
        try {
            return timePeriodRepository.save(period);
        } catch (DataIntegrityViolationException ex) {
            return timePeriodRepository.findByName(name)
                    .map(existing -> synchronizePeriodName(existing, name, userId))
                    .orElseThrow(() -> ex);
        }
    }

    private void populateIncludedMonths(
            AgreementTimePeriod period,
            String name,
            PayoutFrequency frequency,
            int financialYearStartMonth,
            LocalDate anchorDate) {
        List<YearMonth> months = TimePeriodDimensions.includedMonthsFromName(
                name, frequency, financialYearStartMonth, anchorDate);
        for (YearMonth yearMonth : months) {
            AgreementTimePeriodMonth month = AgreementTimePeriodMonth.builder()
                    .timePeriod(period)
                    .calendarMonth(yearMonth.getMonthValue())
                    .calendarYear(yearMonth.getYear())
                    .build();
            period.getIncludedMonths().add(month);
        }
    }
}
