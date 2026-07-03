package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CommitJbpRequest;
import com.medplus.agreement_tracker_backend.dto.request.JbpConfigurationBlockDto;
import com.medplus.agreement_tracker_backend.dto.request.JbpWorkbookRequest;
import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto;
import com.medplus.agreement_tracker_backend.dto.response.JbpStructureHydrationResponse;
import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto.UnpivotedRow;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodSummaryResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpCommercialPeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpConfiguration;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpVersionFrequency;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import com.medplus.agreement_tracker_backend.enums.JbpValueType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpCommercialPeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpConfigurationRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpVersionFrequencyRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.service.AgreementTimePeriodResolutionService;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import com.medplus.agreement_tracker_backend.service.JbpCommercialService;
import com.medplus.agreement_tracker_backend.util.JbpConfigurationCollisionValidator;
import com.medplus.agreement_tracker_backend.util.JbpExcelSheetLayout;
import com.medplus.agreement_tracker_backend.util.JbpTemporalReconciliationUtil;
import com.medplus.agreement_tracker_backend.util.TimePeriodDimensions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JbpCommercialServiceImpl implements JbpCommercialService {

    private static final BigDecimal PERCENT_LIMIT = new BigDecimal("100");

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementJbpConfigurationRepository jbpConfigurationRepository;
    private final AgreementJbpCommercialPeriodRepository jbpCommercialPeriodRepository;
    private final AgreementJbpVersionFrequencyRepository versionFrequencyRepository;
    private final AgreementTimePeriodRepository timePeriodRepository;
    private final CommercialVersionGuard commercialVersionGuard;
    private final AgreementTimePeriodResolutionService periodResolutionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitJbpStructure(Long agreementVersionId, CommitJbpRequest request, Long currentUserId) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        if (request.stagedWorkbook() == null || request.stagedWorkbook().sheets() == null
                || request.stagedWorkbook().sheets().isEmpty()) {
            throw new IncompleteAgreementException("Staged JBP workbook is required.");
        }

        List<String> selectedFrequencies = loadVersionFrequencies(agreementVersionId);
        validateStoredConfigurationCollisions(agreementVersionId, selectedFrequencies);

        Map<Long, AgreementJbpConfiguration> configurationsById =
                jbpConfigurationRepository.findByAgreementVersionId(agreementVersionId).stream()
                        .collect(Collectors.toMap(AgreementJbpConfiguration::getId, Function.identity()));

        jbpCommercialPeriodRepository.deleteByAgreementVersionId(agreementVersionId);

        Map<Long, AgreementTimePeriod> periodsById = loadPeriodsForCommit(request);
        List<AgreementJbpCommercialPeriod> entities = new ArrayList<>();
        Set<String> seenCellCoordinates = new HashSet<>();

        for (var sheet : request.stagedWorkbook().sheets()) {
            validateIncreasingTargets(sheet.rows());
            for (var row : sheet.rows()) {
                AgreementTimePeriod period = periodsById.get(row.timePeriodId());
                if (period == null) {
                    throw new ResourceNotFoundException("AgreementTimePeriod", row.timePeriodId());
                }

                AgreementJbpConfiguration configuration = configurationsById.get(row.jbpConfigurationId());
                if (configuration == null) {
                    throw new BusinessException(
                            "JBP configuration " + row.jbpConfigurationId() + " does not belong to this agreement version.");
                }
                boolean isMasterRow = row.subPeriodName() == null;
                String periodLabel = isMasterRow ? row.parentPeriodName() : row.subPeriodName();

                validateCommitThresholds(sheet, row, periodLabel, isMasterRow);
                validateHighestParentTargetType(sheet, row, periodLabel);
                validateUniqueCellCoordinate(row, periodLabel, sheet.sheetName(), seenCellCoordinates);

                AgreementTimePeriod parentTimePeriod = null;
                if (!isMasterRow && row.parentPeriodId() != null) {
                    parentTimePeriod = periodsById.get(row.parentPeriodId());
                    if (parentTimePeriod == null) {
                        throw new ResourceNotFoundException("AgreementTimePeriod", row.parentPeriodId());
                    }
                }

                entities.add(AgreementJbpCommercialPeriod.builder()
                        .agreementVersion(version)
                        .jbpConfiguration(configuration)
                        .targetType(row.targetType())
                        .target(row.target())
                        .qualifierPercent(row.qualifierPercent() != null ? row.qualifierPercent() : BigDecimal.ZERO)
                        .payoutType(row.payoutType())
                        .payout(row.payout())
                        .maxPurchase(row.maxPurchase())
                        .maxPayout(row.maxPayout())
                        .slabTierNumber(row.slabTierNumber())
                        .timePeriod(period)
                        .parentTimePeriod(parentTimePeriod)
                        .build());
            }
        }

        if (!entities.isEmpty()) {
            jbpCommercialPeriodRepository.saveAll(entities);
        }

        if (request.stagedWorkbook().selectedFrequencies() != null
                && !request.stagedWorkbook().selectedFrequencies().isEmpty()) {
            replaceVersionFrequencies(version, request.stagedWorkbook().selectedFrequencies());
        }
        version.setCommercialStructure(CommercialStructure.SLAB);
        version.setCommercialValue(null);
        version.setFlatValueType(null);
        version.setFlatBaselineFrequency(null);
        version.setUpdatedByUserId(currentUserId);
        agreementVersionRepository.save(version);
    }

    @Override
    @Transactional(readOnly = true)
    public JbpStructureHydrationResponse getJbpStructure(Long agreementVersionId, Long currentUserId) {
        agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        List<String> frequencies = loadVersionFrequencies(agreementVersionId);
        List<JbpConfigurationBlockDto> configurations = jbpConfigurationRepository
                .findHydratedByAgreementVersionId(agreementVersionId).stream()
                .map(this::toConfigurationBlockDto)
                .toList();

        List<AgreementJbpCommercialPeriod> commercialPeriods =
                jbpCommercialPeriodRepository.findHydrationRowsByAgreementVersionId(agreementVersionId);

        if (configurations.isEmpty() && commercialPeriods.isEmpty()) {
            throw new ResourceNotFoundException("JBP structure", agreementVersionId);
        }

        JbpStagedWorkbookDto stagedWorkbook = commercialPeriods.isEmpty()
                ? null
                : rebuildStagedWorkbook(commercialPeriods, frequencies);

        return new JbpStructureHydrationResponse(frequencies, configurations, stagedWorkbook);
    }

    @Override
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public List<TimePeriodSummaryResponse> listAvailablePeriods(
            Long agreementVersionId,
            PayoutFrequency frequency,
            Long currentUserId,
            Integer financialYearStartMonth) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            throw new IncompleteAgreementException("Contract dates must be saved before listing time periods.");
        }

        AgreementSlab probe = AgreementSlab.builder().payoutFrequency(frequency).build();
        return periodResolutionService.resolvePeriodsForSlab(
                        version,
                        probe,
                        currentUserId,
                        financialYearStartMonth).stream()
                .sorted(TimePeriodDimensions.chronologicalComparator())
                .map(period -> {
                    YearMonth earliest = period.earliestIncludedMonth();
                    return new TimePeriodSummaryResponse(
                            period.getId(),
                            period.getName(),
                            period.getPeriodFrequency() != null ? period.getPeriodFrequency().name() : null,
                            earliest != null ? earliest.getYear() : null,
                            earliest != null ? earliest.getMonthValue() : null);
                })
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistWorkbookMetadata(AgreementVersion version, JbpWorkbookRequest request, Long currentUserId) {
        JbpConfigurationCollisionValidator.validateNoParentPeriodOverlap(
                request.configurations(),
                this::resolvePeriodName);
        replaceVersionFrequencies(version, request.selectedFrequencies());
        version.setUpdatedByUserId(currentUserId);
        agreementVersionRepository.save(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistConfigurationBlock(
            AgreementJbpConfiguration configuration,
            JbpConfigurationBlockDto config,
            List<AgreementTimePeriod> parentPeriods) {
        configuration.setSelectedPeriods(new LinkedHashSet<>(parentPeriods));
        jbpConfigurationRepository.save(configuration);
    }

    public List<PayoutFrequency> resolveSubFrequencies(List<String> selectedFrequencies, PayoutFrequency masterFrequency) {
        int masterRank = JbpTemporalReconciliationUtil.frequencyRank(masterFrequency);
        List<PayoutFrequency> subFrequencies = new ArrayList<>();
        for (String raw : selectedFrequencies) {
            PayoutFrequency frequency = PayoutFrequency.valueOf(raw.trim().toUpperCase());
            if (JbpTemporalReconciliationUtil.frequencyRank(frequency) < masterRank) {
                subFrequencies.add(frequency);
            }
        }
        subFrequencies.sort(Comparator.comparingInt(JbpTemporalReconciliationUtil::frequencyRank).reversed());
        return subFrequencies;
    }

    public PayoutFrequency resolveMasterFrequency(List<String> selectedFrequencies) {
        return selectedFrequencies.stream()
                .map(value -> PayoutFrequency.valueOf(value.trim().toUpperCase()))
                .max(Comparator.comparingInt(JbpTemporalReconciliationUtil::frequencyRank))
                .orElseThrow(() -> new IncompleteAgreementException("Select at least one target frequency."));
    }

    public List<AgreementTimePeriod> resolveSubPeriodsForConfig(
            AgreementVersion version,
            List<AgreementTimePeriod> parentPeriods,
            PayoutFrequency subFrequency,
            Long userId) {
        AgreementSlab probe = AgreementSlab.builder().payoutFrequency(subFrequency).build();
        return periodResolutionService.resolvePeriodsForSlab(version, probe, userId).stream()
                .filter(sub -> JbpTemporalReconciliationUtil.resolveParentPeriod(sub, parentPeriods).isPresent())
                .sorted(TimePeriodDimensions.chronologicalComparator())
                .toList();
    }

    private Map<Long, AgreementTimePeriod> loadPeriodsForCommit(CommitJbpRequest request) {
        Set<Long> periodIds = new HashSet<>();
        for (var sheet : request.stagedWorkbook().sheets()) {
            for (var row : sheet.rows()) {
                periodIds.add(row.timePeriodId());
                if (row.parentPeriodId() != null) {
                    periodIds.add(row.parentPeriodId());
                }
            }
        }
        return timePeriodRepository.findAllById(periodIds).stream()
                .collect(Collectors.toMap(AgreementTimePeriod::getId, Function.identity()));
    }

    private void validateUniqueCellCoordinate(
            UnpivotedRow row,
            String periodLabel,
            String sheetName,
            Set<String> seenCellCoordinates) {
        String coordinate = row.jbpConfigurationId() + "-" + row.slabTierNumber() + "-" + row.timePeriodId();
        if (!seenCellCoordinates.add(coordinate)) {
            throw new BusinessException(String.format(
                    "Duplicate JBP cell on sheet '%s' for %s (%s): configuration %d, tier %d, period %d.",
                    sheetName,
                    periodLabel,
                    row.slabTierLabel(),
                    row.jbpConfigurationId(),
                    row.slabTierNumber(),
                    row.timePeriodId()));
        }
    }

    private void validateHighestParentTargetType(
            JbpStagedWorkbookDto.StagedSheet sheet,
            UnpivotedRow row,
            String periodLabel) {
        if (!sheet.master() || row.targetType() != JbpValueType.RELATIVE) {
            return;
        }
        throw new BusinessException(String.format(
                "Data Integrity Violation: The interval [%s] is the highest parent tier in this configuration. "
                        + "Its Target Type must be strictly ABSOLUTE, but the staged workbook contains RELATIVE for %s.",
                periodLabel,
                row.slabTierLabel()));
    }

    private void validateCommitThresholds(
            JbpStagedWorkbookDto.StagedSheet sheet,
            UnpivotedRow row,
            String periodLabel,
            boolean isMasterRow) {
        if (row.targetType() == null || row.target() == null) {
            throw new IncompleteAgreementException(String.format(
                    "Target Type and Target are required for %s (%s).",
                    periodLabel,
                    row.slabTierLabel()));
        }
        if (isMasterRow) {
            if (row.payoutType() == null || row.payout() == null) {
                throw new BusinessException(String.format(
                        "Data Integrity Violation: The Highest Parent interval [%s] must have a defined Payout Type and Payout.",
                        periodLabel));
            }
        } else {
            validateSubPeriodSignificance(periodLabel, row);
        }
        if (row.qualifierPercent() != null && row.qualifierPercent().compareTo(PERCENT_LIMIT) > 0) {
            throw new IncompleteAgreementException(String.format(
                    "Qualifier %% must be <= 100 for %s (%s).",
                    periodLabel,
                    row.slabTierLabel()));
        }
        if (row.targetType() == JbpValueType.RELATIVE && row.target().compareTo(PERCENT_LIMIT) > 0) {
            throw new IncompleteAgreementException(String.format(
                    "Relative Target must be <= 100 for %s (%s).",
                    periodLabel,
                    row.slabTierLabel()));
        }
        if (row.payoutType() == JbpValueType.RELATIVE
                && row.payout() != null
                && row.payout().compareTo(PERCENT_LIMIT) > 0) {
            throw new IncompleteAgreementException(String.format(
                    "Relative Payout must be <= 100 for %s (%s).",
                    periodLabel,
                    row.slabTierLabel()));
        }
    }

    private void validateSubPeriodSignificance(String intervalName, UnpivotedRow row) {
        boolean hasPayout = row.payoutType() != null
                && row.payout() != null
                && row.payout().compareTo(BigDecimal.ZERO) > 0;
        boolean hasQualifier = row.qualifierPercent() != null
                && row.qualifierPercent().compareTo(BigDecimal.ZERO) > 0;
        if (hasPayout || hasQualifier) {
            return;
        }
        throw new BusinessException(String.format(
                "Business Rule Violation: Sub-period [%s] has no Payout and a Qualifier of 0%%. "
                        + "Sub-periods must have either a Payout or a Qualifier percentage to be contractually significant.",
                intervalName));
    }

    private void validateIncreasingTargets(List<UnpivotedRow> rows) {
        Map<String, List<UnpivotedRow>> rowsByGroup = rows.stream()
                .filter(row -> row.target() != null)
                .collect(Collectors.groupingBy(row -> row.timePeriodId() + "::" + row.jbpConfigurationId()));

        for (List<UnpivotedRow> groupRows : rowsByGroup.values()) {
            List<UnpivotedRow> sorted = groupRows.stream()
                    .sorted(Comparator.comparing(UnpivotedRow::slabTierNumber))
                    .toList();
            for (int index = 1; index < sorted.size(); index++) {
                UnpivotedRow previous = sorted.get(index - 1);
                UnpivotedRow current = sorted.get(index);
                if (current.target().compareTo(previous.target()) <= 0) {
                    String periodLabel = current.subPeriodName() != null
                            ? current.subPeriodName()
                            : current.parentPeriodName();
                    throw new IncompleteAgreementException(String.format(
                            "Threshold Error in %s (%s): Target (%s) must be greater than %s target (%s).",
                            periodLabel,
                            current.slabTierLabel(),
                            current.target().toPlainString(),
                            previous.slabTierLabel(),
                            previous.target().toPlainString()));
                }
            }
        }
    }

    private void validateStoredConfigurationCollisions(
            Long agreementVersionId,
            List<String> selectedFrequencies) {
        List<JbpConfigurationBlockDto> configurations = loadStoredConfigurations(agreementVersionId, selectedFrequencies);
        JbpConfigurationCollisionValidator.validateNoParentPeriodOverlap(
                configurations,
                this::resolvePeriodName);
    }

    private List<JbpConfigurationBlockDto> loadStoredConfigurations(
            Long agreementVersionId,
            List<String> selectedFrequencies) {
        List<JbpConfigurationBlockDto> configurations = jbpConfigurationRepository
                .findHydratedByAgreementVersionId(agreementVersionId).stream()
                .map(this::toConfigurationBlockDto)
                .toList();
        if (configurations.isEmpty()) {
            throw new IncompleteAgreementException(
                    "Generate the JBP workbook template before committing JBP structure.");
        }
        return configurations;
    }

    private JbpConfigurationBlockDto toConfigurationBlockDto(AgreementJbpConfiguration configuration) {
        List<Long> parentPeriodIds = configuration.getSelectedPeriods().stream()
                .map(AgreementTimePeriod::getId)
                .sorted()
                .toList();
        return new JbpConfigurationBlockDto(
                String.valueOf(configuration.getId()),
                parentPeriodIds,
                configuration.getSlabCount());
    }

    private List<String> loadVersionFrequencies(Long agreementVersionId) {
        return versionFrequencyRepository.findByAgreementVersionIdOrderByFrequencyAsc(agreementVersionId).stream()
                .map(frequency -> frequency.getFrequency().name())
                .toList();
    }

    private void replaceVersionFrequencies(AgreementVersion version, List<String> selectedFrequencies) {
        versionFrequencyRepository.deleteByAgreementVersionId(version.getId());
        if (selectedFrequencies == null) {
            return;
        }
        for (String raw : selectedFrequencies) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            PayoutFrequency frequency = PayoutFrequency.valueOf(raw.trim().toUpperCase());
            versionFrequencyRepository.save(AgreementJbpVersionFrequency.builder()
                    .agreementVersion(version)
                    .frequency(frequency)
                    .build());
        }
    }

    private String resolvePeriodName(Long periodId) {
        return timePeriodRepository.findById(periodId)
                .map(AgreementTimePeriod::getName)
                .orElse("ID " + periodId);
    }

    private JbpStagedWorkbookDto rebuildStagedWorkbook(
            List<AgreementJbpCommercialPeriod> commercialPeriods,
            List<String> frequencies) {
        Map<String, HydratedSheetBucket> sheetBuckets = new LinkedHashMap<>();
        for (AgreementJbpCommercialPeriod period : commercialPeriods) {
            HydratedSheetBucket bucket = resolveSheetBucket(period);
            sheetBuckets.computeIfAbsent(bucket.sheetName(), ignored -> bucket).rows().add(toHydratedRow(period));
        }

        List<JbpStagedWorkbookDto.StagedSheet> sheets = sheetBuckets.values().stream()
                .sorted(Comparator
                        .comparing(HydratedSheetBucket::master).reversed()
                        .thenComparing(bucket -> JbpTemporalReconciliationUtil.frequencyRank(bucket.frequency()),
                                Comparator.reverseOrder()))
                .map(bucket -> {
                    List<UnpivotedRow> rows = sortHydrationRows(bucket.rows());
                    applyFirstInParentGroupFlags(rows);
                    return new JbpStagedWorkbookDto.StagedSheet(
                            null,
                            bucket.sheetName(),
                            bucket.sheetName(),
                            bucket.frequency().name(),
                            bucket.master(),
                            rows);
                })
                .toList();

        return new JbpStagedWorkbookDto(sheets, frequencies, false, null);
    }

    private HydratedSheetBucket resolveSheetBucket(AgreementJbpCommercialPeriod period) {
        AgreementTimePeriod timePeriod = period.getTimePeriod();
        if (period.getParentTimePeriod() == null) {
            PayoutFrequency frequency = timePeriod.getPeriodFrequency();
            String sheetName = JbpExcelSheetLayout.canonicalMasterSheetName(frequency);
            return new HydratedSheetBucket(sheetName, frequency, true, new ArrayList<>());
        }

        PayoutFrequency frequency = timePeriod.getPeriodFrequency();
        String sheetName = JbpExcelSheetLayout.canonicalSpreadSheetName(frequency);
        return new HydratedSheetBucket(sheetName, frequency, false, new ArrayList<>());
    }

    private UnpivotedRow toHydratedRow(AgreementJbpCommercialPeriod period) {
        AgreementTimePeriod timePeriod = period.getTimePeriod();
        AgreementTimePeriod parentTimePeriod = period.getParentTimePeriod();
        boolean masterRow = parentTimePeriod == null;
        String parentPeriodName = masterRow ? timePeriod.getName() : parentTimePeriod.getName();
        Long parentPeriodId = masterRow ? timePeriod.getId() : parentTimePeriod.getId();
        String subPeriodName = masterRow ? null : timePeriod.getName();
        String slabTierLabel = "Slab " + period.getSlabTierNumber();

        return new UnpivotedRow(
                parentPeriodName,
                parentPeriodId,
                subPeriodName,
                timePeriod.getId(),
                period.getSlabTierNumber(),
                period.getJbpConfiguration().getId(),
                slabTierLabel,
                period.getTargetType(),
                period.getTarget(),
                period.getQualifierPercent(),
                period.getPayoutType(),
                period.getPayout(),
                period.getMaxPurchase(),
                period.getMaxPayout(),
                false);
    }

    private List<UnpivotedRow> sortHydrationRows(List<UnpivotedRow> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparing(UnpivotedRow::parentPeriodName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(UnpivotedRow::subPeriodName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(UnpivotedRow::slabTierNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(UnpivotedRow::jbpConfigurationId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void applyFirstInParentGroupFlags(List<UnpivotedRow> rows) {
        String lastParent = null;
        for (int index = 0; index < rows.size(); index++) {
            UnpivotedRow row = rows.get(index);
            boolean firstInGroup = !row.parentPeriodName().equals(lastParent);
            if (firstInGroup != row.firstInParentGroup()) {
                rows.set(index, copyHydratedRow(row, firstInGroup));
            }
            lastParent = row.parentPeriodName();
        }
    }

    private UnpivotedRow copyHydratedRow(UnpivotedRow row, boolean firstInGroup) {
        return new UnpivotedRow(
                row.parentPeriodName(),
                row.parentPeriodId(),
                row.subPeriodName(),
                row.timePeriodId(),
                row.slabTierNumber(),
                row.jbpConfigurationId(),
                row.slabTierLabel(),
                row.targetType(),
                row.target(),
                row.qualifierPercent(),
                row.payoutType(),
                row.payout(),
                row.maxPurchase(),
                row.maxPayout(),
                firstInGroup);
    }

    private record HydratedSheetBucket(
            String sheetName,
            PayoutFrequency frequency,
            boolean master,
            List<UnpivotedRow> rows) {
    }
}
