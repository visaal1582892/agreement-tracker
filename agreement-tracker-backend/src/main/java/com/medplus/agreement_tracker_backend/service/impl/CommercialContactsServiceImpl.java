package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CommitCutoffsRequest;
import com.medplus.agreement_tracker_backend.dto.response.SlabPeriodCutoffMatrixResponse;
import com.medplus.agreement_tracker_backend.dto.response.SlabPeriodCutoffRowResponse;
import com.medplus.agreement_tracker_backend.dto.response.StagedCutoffMatrixResponse;
import com.medplus.agreement_tracker_backend.dto.response.StagedMatrixRowDto;
import com.medplus.agreement_tracker_backend.dto.response.StagedSlabHeaderDto;
import com.medplus.agreement_tracker_backend.dto.response.StagedTierCutoffDto;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpCommercialPeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpConfigurationRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpVersionFrequencyRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.service.AgreementTimePeriodResolutionService;
import com.medplus.agreement_tracker_backend.service.CommercialContactsService;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import com.medplus.agreement_tracker_backend.util.ExcelCellReader;
import com.medplus.agreement_tracker_backend.util.TimePeriodDimensions;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommercialContactsServiceImpl implements CommercialContactsService {

    private static final int METADATA_ROW = 0;
    private static final int SUPER_HEADER_ROW = 1;
    private static final int SUB_HEADER_ROW = 2;
    private static final int DATA_START_ROW = 3;

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementSlabRepository slabRepository;
    private final AgreementJbpCommercialPeriodRepository jbpCommercialPeriodRepository;
    private final AgreementJbpConfigurationRepository jbpConfigurationRepository;
    private final AgreementJbpVersionFrequencyRepository jbpVersionFrequencyRepository;
    private final AgreementTimePeriodRepository timePeriodRepository;
    private final CommercialVersionGuard commercialVersionGuard;
    private final AgreementTimePeriodResolutionService periodResolutionService;

    @Override
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public byte[] generateCutoffTemplate(Long agreementVersionId, Long currentUserId) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        validateContractDates(version);
        List<AgreementSlab> slabs = slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementVersionId);
        if (slabs.isEmpty()) {
            throw new IncompleteAgreementException("Add at least one slab tier before downloading the cutoff template.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Cutoff Template");
            CellStyle lockedStyle = workbook.createCellStyle();
            lockedStyle.setLocked(true);
            CellStyle editableStyle = workbook.createCellStyle();
            editableStyle.setLocked(false);

            Row metadataRow = sheet.createRow(METADATA_ROW);
            Row superHeaderRow = sheet.createRow(SUPER_HEADER_ROW);
            Row subHeaderRow = sheet.createRow(SUB_HEADER_ROW);

            setStringCell(superHeaderRow, 0, "Time Period", lockedStyle);
            setStringCell(subHeaderRow, 0, "Period Name", lockedStyle);

            int col = 1;
            for (AgreementSlab slab : slabs) {
                setNumericCell(metadataRow, col, slab.getId(), lockedStyle);
                setNumericCell(metadataRow, col + 1, slab.getId(), lockedStyle);
                sheet.addMergedRegion(new CellRangeAddress(SUPER_HEADER_ROW, SUPER_HEADER_ROW, col, col + 1));
                setStringCell(superHeaderRow, col, formatSuperHeader(slab), lockedStyle);
                setStringCell(subHeaderRow, col, "Lower Cutoff %", editableStyle);
                setStringCell(subHeaderRow, col + 1, "Upper Cutoff %", editableStyle);
                col += 2;
            }

            LinkedHashMap<Long, AgreementTimePeriod> uniquePeriods = new LinkedHashMap<>();
            for (AgreementSlab slab : slabs) {
                for (AgreementTimePeriod period : periodResolutionService.resolvePeriodsForSlab(
                        version, slab, currentUserId)) {
                    uniquePeriods.putIfAbsent(period.getId(), period);
                }
            }

            List<AgreementTimePeriod> sortedPeriods = uniquePeriods.values().stream()
                    .sorted(TimePeriodDimensions.chronologicalComparator())
                    .toList();

            int dataRowIndex = DATA_START_ROW;
            for (AgreementTimePeriod period : sortedPeriods) {
                Row dataRow = sheet.createRow(dataRowIndex++);
                setStringCell(dataRow, 0, period.getName(), lockedStyle);
                for (int cutoffCol = 1; cutoffCol < col; cutoffCol++) {
                    setStringCell(dataRow, cutoffCol, "", editableStyle);
                }
            }

            metadataRow.setZeroHeight(true);
            for (int i = 0; i < col; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.protectSheet("agreement-tracker");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate cutoff template", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StagedCutoffMatrixResponse parseCutoffUpload(
            Long agreementVersionId,
            MultipartFile file,
            Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new IncompleteAgreementException("Uploaded Excel file is required.");
        }
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        validateContractDates(version);

        List<String> validationErrors = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IncompleteAgreementException("Excel file does not contain any worksheets.");
            }

            Row metadataRow = sheet.getRow(METADATA_ROW);
            Row superHeaderRow = sheet.getRow(SUPER_HEADER_ROW);
            if (metadataRow == null || superHeaderRow == null) {
                throw new IncompleteAgreementException("Invalid template format: missing header rows.");
            }

            List<SlabColumnPair> slabColumns = new ArrayList<>();
            List<StagedSlabHeaderDto> slabHeaders = new ArrayList<>();
            int lastCol = Math.max(superHeaderRow.getLastCellNum(), metadataRow.getLastCellNum());
            for (int col = 1; col < lastCol; col += 2) {
                Long slabId = readLong(metadataRow.getCell(col));
                if (slabId == null) {
                    continue;
                }
                String displayTitle = readString(superHeaderRow.getCell(col));
                if (displayTitle == null || displayTitle.isBlank()) {
                    displayTitle = "Tier " + slabId;
                }
                slabColumns.add(new SlabColumnPair(slabId, col, col + 1));
                slabHeaders.add(new StagedSlabHeaderDto(slabId, displayTitle));
            }

            if (slabColumns.isEmpty()) {
                throw new IncompleteAgreementException("No slab columns found in uploaded template.");
            }

            List<StagedMatrixRowDto> matrixRows = new ArrayList<>();
            for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String timePeriodName = readString(row.getCell(0));
                if (timePeriodName == null || timePeriodName.isBlank()) {
                    continue;
                }

                Long timePeriodId = resolvePeriodIdByName(timePeriodName);
                Map<String, StagedTierCutoffDto> tierCutoffs = new LinkedHashMap<>();
                int displayRow = rowIndex + 1;

                for (SlabColumnPair pair : slabColumns) {
                    BigDecimal lower = ExcelCellReader.readAsDecimal(row.getCell(pair.lowerCol()));
                    BigDecimal upper = ExcelCellReader.readAsDecimal(row.getCell(pair.upperCol()));

                    if (lower != null && lower.compareTo(new BigDecimal("100")) > 0) {
                        validationErrors.add("Row " + displayRow + ": Lower cutoff must be <= 100");
                    }
                    if (upper != null && upper.compareTo(new BigDecimal("100")) < 0) {
                        validationErrors.add("Row " + displayRow + ": Upper cutoff must be >= 100");
                    }
                    if (lower == null && upper == null) {
                        continue;
                    }
                    tierCutoffs.put(
                            String.valueOf(pair.slabId()),
                            new StagedTierCutoffDto(pair.slabId(), lower, upper));
                }

                matrixRows.add(new StagedMatrixRowDto(timePeriodName, timePeriodId, tierCutoffs));
            }

            if (!validationErrors.isEmpty()) {
                throw new BusinessException(String.join("; ", validationErrors));
            }

            return new StagedCutoffMatrixResponse(matrixRows, slabHeaders);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read cutoff upload file", ex);
        }
    }

    @Override
    @Transactional
    public void commitCutoffs(Long agreementVersionId, CommitCutoffsRequest request, Long currentUserId) {
        throw new BusinessException("Slab period cutoffs are managed via JBP commercial structure.");
    }

    @Override
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public SlabPeriodCutoffMatrixResponse listCutoffMatrix(Long agreementVersionId, Long currentUserId) {
        AgreementVersion version = loadVersionForRead(agreementVersionId, currentUserId);
        List<AgreementSlab> slabs = slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementVersionId);

        List<SlabPeriodCutoffRowResponse> rows = new ArrayList<>();
        for (AgreementSlab slab : slabs) {
            List<AgreementTimePeriod> periods = periodResolutionService.resolvePeriodsForSlab(
                    version, slab, currentUserId);
            for (AgreementTimePeriod period : periods) {
                YearMonth earliest = period.earliestIncludedMonth();
                rows.add(new SlabPeriodCutoffRowResponse(
                        period.getId(),
                        period.getName(),
                        earliest != null ? earliest.getYear() : null,
                        earliest != null ? earliest.getMonthValue() : null,
                        slab.getId(),
                        formatSuperHeader(slab),
                        slab.getMinCap(),
                        slab.getMaxCap(),
                        null,
                        null,
                        slab.getCommercialValue(),
                        slab.getValueType() != null ? slab.getValueType().name() : null));
            }
        }
        rows.sort(Comparator
                .comparing(SlabPeriodCutoffRowResponse::periodYear, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SlabPeriodCutoffRowResponse::monthNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SlabPeriodCutoffRowResponse::slabTierLabel));
        return new SlabPeriodCutoffMatrixResponse(rows, true);
    }

    @Override
    @Transactional
    public void purgeCommercialStructureData(Long agreementVersionId, Long currentUserId) {
        commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        jbpCommercialPeriodRepository.deleteByAgreementVersionId(agreementVersionId);
        jbpConfigurationRepository.deleteByAgreementVersionId(agreementVersionId);
        jbpVersionFrequencyRepository.deleteByAgreementVersionId(agreementVersionId);
        slabRepository.deleteByAgreementVersionId(agreementVersionId);
    }

    private Long resolvePeriodIdByName(String periodName) {
        return timePeriodRepository.findByName(periodName)
                .map(AgreementTimePeriod::getId)
                .orElse(null);
    }

    private AgreementVersion loadVersionForRead(Long agreementVersionId, Long userId) {
        return agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
    }

    private void validateContractDates(AgreementVersion version) {
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            throw new IncompleteAgreementException("Contract start and expiry dates are required.");
        }
    }

    private static String formatSuperHeader(AgreementSlab slab) {
        return "Tier: " + formatTargetValue(slab) + " (" + formatPayoutDisplay(slab) + ")";
    }

    private static String formatTargetValue(AgreementSlab slab) {
        return slab.getMinCap().stripTrailingZeros().toPlainString();
    }

    private static String formatPayoutDisplay(AgreementSlab slab) {
        if (slab.getValueType() == SlabValueType.PERCENTAGE) {
            return slab.getCommercialValue().stripTrailingZeros().toPlainString() + "%";
        }
        return slab.getCommercialValue().stripTrailingZeros().toPlainString();
    }

    private static void setStringCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setNumericCell(Row row, int index, Long value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static Long readLong(Cell cell) {
        BigDecimal decimal = ExcelCellReader.readAsDecimal(cell);
        return decimal != null ? decimal.longValue() : null;
    }

    private static String readString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private record SlabColumnPair(Long slabId, int lowerCol, int upperCol) {}
}
