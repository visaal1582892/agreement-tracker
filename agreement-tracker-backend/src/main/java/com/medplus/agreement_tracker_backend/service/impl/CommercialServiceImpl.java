package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CommercialTemplateRequest;
import com.medplus.agreement_tracker_backend.dto.request.CommercialTypeSwitchRequest;
import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.request.UpsertTargetRequest;
import com.medplus.agreement_tracker_backend.dto.response.CommercialUploadResponse;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodTargetsPreviewResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.CommercialTypeSwitchAction;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.service.AgreementTimePeriodResolutionService;
import com.medplus.agreement_tracker_backend.service.CommercialService;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import com.medplus.agreement_tracker_backend.util.ExcelCellReader;
import com.medplus.agreement_tracker_backend.util.SlabHeaderFormatter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommercialServiceImpl implements CommercialService {

    private static final Set<String> VALID_FREQUENCIES =
            Set.of("ONE_TIME", "MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY");

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementSlabRepository slabRepository;
    private final CommercialVersionGuard commercialVersionGuard;
    private final AgreementTimePeriodResolutionService periodResolutionService;

    @Override
    @Transactional
    public byte[] generateCommercialTemplate(Long agreementId, CommercialTemplateRequest request, Long currentUserId) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);

        validateAgreementDates(version);
        validateFrequencies(request.selectedFrequencies());
        commercialVersionGuard.validateQpsPayoutFrequencies(version, request.selectedFrequencies());

        CommercialSlabType slabType = resolveSlabType(request.slabType());
        List<AgreementSlab> slabs = slabRepository
                .findByAgreementVersionIdAndSlabTypeOrderByMinCapAsc(agreementId, slabType);
        if (slabs.isEmpty()) {
            throw new IncompleteAgreementException(
                    "No slabs found for this agreement. Add at least one slab before generating the template.");
        }

        LocalDate startDate = version.getStartDate();
        LocalDate expiryDate = version.getExpiryDate();
        int financialYearStartMonth = com.medplus.agreement_tracker_backend.util.DynamicFinancialYearPeriodGenerator
                .resolveStartMonth(version.getFinancialYearStartMonth());
        List<String> groupedPeriodNames = buildGroupedPeriodNames(
                request.selectedFrequencies(), startDate, expiryDate, financialYearStartMonth, currentUserId);

        List<SlabDTO> slabDtos = slabs.stream()
                .map(slab -> new SlabDTO(
                        slab.getMinCap(),
                        slab.getMaxCap(),
                        slab.getCapUnit(),
                        slab.getValueType(),
                        slab.getCommercialValue(),
                        slab.getPayoutFrequency(),
                        slab.getSlabType()))
                .toList();

        return buildExcel(slabDtos, groupedPeriodNames);
    }

    @Override
    @Transactional
    public CommercialUploadResponse uploadCommercialTargets(
            Long agreementId, MultipartFile file, CommercialSlabType slabType, Long currentUserId) {
        throw new BusinessException("Commercial targets are managed via JBP commercial periods.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimePeriodTargetsPreviewResponse> getTargetsPreview(Long agreementId, Long currentUserId) {
        loadVersionForRead(agreementId, currentUserId);
        return List.of();
    }

    @Override
    @Transactional
    public void upsertTarget(Long agreementId, UpsertTargetRequest request, Long currentUserId) {
        throw new BusinessException("Commercial targets are managed via JBP commercial periods.");
    }

    @Override
    @Transactional
    public void switchCommercialType(Long agreementId, CommercialTypeSwitchRequest request, Long currentUserId) {
        commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);

        if (request.action() == CommercialTypeSwitchAction.CLEAR) {
            slabRepository.deleteByAgreementVersionId(agreementId);
            return;
        }

        CommercialSlabType newSlabType = request.newSlabType();
        List<AgreementSlab> slabs = slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementId);
        for (AgreementSlab slab : slabs) {
            slab.setSlabType(newSlabType);
            slab.setUpdatedByUserId(currentUserId);
        }
        slabRepository.saveAll(slabs);
    }

    private CommercialSlabType resolveSlabType(CommercialSlabType slabType) {
        return slabType != null ? slabType : CommercialSlabType.PURCHASE;
    }

    private AgreementVersion loadVersionForRead(Long agreementVersionId, Long userId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        if (version.getApprovalStatus() == ApprovalStatus.DRAFT
                && !version.getAgreement().getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("AgreementVersion", agreementVersionId);
        }
        return version;
    }

    private void validateAgreementDates(AgreementVersion version) {
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            throw new IncompleteAgreementException("Agreement start and expiry dates must be set.");
        }
    }

    private void validateFrequencies(List<String> frequencies) {
        if (frequencies == null || frequencies.isEmpty()) {
            throw new IncompleteAgreementException("Select at least one payout frequency.");
        }
        for (String frequency : frequencies) {
            if (!VALID_FREQUENCIES.contains(frequency)) {
                throw new IncompleteAgreementException("Invalid payout frequency: " + frequency);
            }
        }
    }

    private List<String> buildGroupedPeriodNames(
            List<String> selectedFrequencies,
            LocalDate startDate,
            LocalDate expiryDate,
            int financialYearStartMonth,
            Long userId) {
        Map<String, String> uniqueNames = new HashMap<>();
        AgreementVersion probeVersion = AgreementVersion.builder()
                .startDate(startDate)
                .expiryDate(expiryDate)
                .financialYearStartMonth(financialYearStartMonth)
                .build();

        for (String rawFrequency : selectedFrequencies) {
            AgreementSlab probeSlab = AgreementSlab.builder()
                    .payoutFrequency(com.medplus.agreement_tracker_backend.enums.PayoutFrequency.valueOf(rawFrequency))
                    .build();
            periodResolutionService.resolvePeriodsForSlab(probeVersion, probeSlab, userId, financialYearStartMonth)
                    .forEach(period -> uniqueNames.putIfAbsent(period.getName(), period.getName()));
        }

        return new ArrayList<>(uniqueNames.keySet());
    }

    private byte[] buildExcel(List<SlabDTO> slabs, List<String> periodNames) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Commercial Targets");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Time Period");

            for (int index = 0; index < slabs.size(); index++) {
                headerRow.createCell(index + 1).setCellValue(SlabHeaderFormatter.format(slabs.get(index)));
            }

            for (int rowIndex = 0; rowIndex < periodNames.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(periodNames.get(rowIndex));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate commercial template", ex);
        }
    }
}
