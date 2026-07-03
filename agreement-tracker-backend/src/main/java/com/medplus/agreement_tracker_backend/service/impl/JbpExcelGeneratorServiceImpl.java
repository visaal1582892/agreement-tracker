package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.JbpConfigurationBlockDto;
import com.medplus.agreement_tracker_backend.dto.request.JbpWorkbookRequest;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpConfiguration;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpCommercialPeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpConfigurationRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.service.AgreementTimePeriodResolutionService;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import com.medplus.agreement_tracker_backend.service.JbpExcelGeneratorService;
import com.medplus.agreement_tracker_backend.util.DynamicFinancialYearPeriodGenerator;
import com.medplus.agreement_tracker_backend.util.JbpConfigurationCollisionValidator;
import com.medplus.agreement_tracker_backend.util.JbpExcelSheetLayout;
import com.medplus.agreement_tracker_backend.util.JbpExcelSheetLayout.Columns;
import com.medplus.agreement_tracker_backend.util.JbpTemporalReconciliationUtil;
import com.medplus.agreement_tracker_backend.util.TimePeriodDimensions;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JbpExcelGeneratorServiceImpl implements JbpExcelGeneratorService {

    private static final int HEADER_ROW = 0;
    private static final int DATA_START_ROW = 1;
    private static final int VALIDATION_END_ROW = 5000;

    private static final Set<PayoutFrequency> JBP_FREQUENCIES = Set.of(
            PayoutFrequency.YEARLY,
            PayoutFrequency.HALF_YEARLY,
            PayoutFrequency.QUARTERLY,
            PayoutFrequency.MONTHLY
    );

    private final AgreementJbpConfigurationRepository jbpConfigurationRepository;
    private final AgreementJbpCommercialPeriodRepository jbpCommercialPeriodRepository;
    private final AgreementTimePeriodRepository timePeriodRepository;
    private final CommercialVersionGuard commercialVersionGuard;
    private final AgreementTimePeriodResolutionService periodResolutionService;
    private final JbpCommercialServiceImpl jbpCommercialService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public byte[] generateWorkbook(
            Long agreementVersionId,
            JbpWorkbookRequest request,
            Long currentUserId,
            Integer startMonthOverride) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        validateContractDates(version);
        validateRequest(request);

        int financialYearStartMonth = DynamicFinancialYearPeriodGenerator.resolveStartMonth(
                startMonthOverride != null
                        ? startMonthOverride
                        : request.financialYearStartMonth() != null
                                ? request.financialYearStartMonth()
                                : version.getFinancialYearStartMonth());
        version.setFinancialYearStartMonth(financialYearStartMonth);

        jbpCommercialService.persistWorkbookMetadata(version, request, currentUserId);
        jbpCommercialPeriodRepository.deleteByAgreementVersionId(version.getId());
        jbpConfigurationRepository.deleteByAgreementVersionId(version.getId());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle lockedStyle = createLockedStyle(workbook);
            CellStyle editableStyle = createEditableStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook, lockedStyle);

            createInstructionsSheet(workbook, lockedStyle, headerStyle);

            Map<String, MasterSheetState> masterSheets = new LinkedHashMap<>();
            Map<String, SpreadSheetState> spreadSheets = new LinkedHashMap<>();

            for (JbpConfigurationBlockDto config : request.configurations()) {
                List<AgreementTimePeriod> parentPeriods = loadParentPeriods(
                        version, config.parentPeriodIds(), financialYearStartMonth, currentUserId);
                PayoutFrequency masterFrequency = parentPeriods.get(0).getPeriodFrequency();
                AgreementJbpConfiguration jbpConfiguration = provisionJbpConfiguration(
                        version, masterFrequency, config, parentPeriods);
                Columns masterLayout = JbpExcelSheetLayout.forSheet(true);
                String masterSheetName = JbpExcelSheetLayout.canonicalMasterSheetName(masterFrequency);

                MasterSheetState masterState = masterSheets.computeIfAbsent(
                        masterSheetName,
                        name -> {
                            Sheet sheet = workbook.createSheet(name);
                            writeHeaderRow(sheet, masterLayout.headers(), headerStyle);
                            return new MasterSheetState(sheet, DATA_START_ROW);
                        });
                appendMasterRows(
                        masterState,
                        masterLayout,
                        parentPeriods,
                        jbpConfiguration,
                        lockedStyle,
                        editableStyle);

                List<PayoutFrequency> subFrequencies = jbpCommercialService.resolveSubFrequencies(
                        request.selectedFrequencies(), masterFrequency);
                for (PayoutFrequency subFrequency : subFrequencies) {
                    List<AgreementTimePeriod> subPeriods = jbpCommercialService.resolveSubPeriodsForConfig(
                            version, parentPeriods, subFrequency, currentUserId);
                    if (subPeriods.isEmpty()) {
                        continue;
                    }
                    Columns spreadLayout = JbpExcelSheetLayout.forSheet(false);
                    String spreadSheetName = JbpExcelSheetLayout.canonicalSpreadSheetName(subFrequency);
                    SpreadSheetState spreadState = spreadSheets.computeIfAbsent(
                            spreadSheetName,
                            name -> createSpreadSheet(workbook, name, spreadLayout, headerStyle));
                    appendSpreadRows(
                            spreadState,
                            spreadLayout,
                            parentPeriods,
                            subPeriods,
                            jbpConfiguration,
                            lockedStyle,
                            editableStyle);
                }
            }

            masterSheets.forEach((name, state) -> {
                Columns layout = JbpExcelSheetLayout.forSheet(true);
                finalizeSheet(state.sheet(), layout, workbook);
            });
            spreadSheets.forEach((name, state) -> {
                Columns layout = JbpExcelSheetLayout.forSheet(false);
                finalizeSheet(state.sheet(), layout, workbook);
            });

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate JBP workbook", ex);
        }
    }

    private void createInstructionsSheet(Workbook workbook, CellStyle lockedStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Instructions");
        CellStyle titleStyle = createHeaderStyle(workbook, lockedStyle);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle sectionStyle = createHeaderStyle(workbook, lockedStyle);
        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionStyle.setFont(sectionFont);

        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        setStringCell(titleRow, 0, "JBP Threshold Workbook Instructions", titleStyle);

        rowIndex = writeInstructionSection(sheet, rowIndex, "General", new String[]{
                "Do not modify hidden columns (Entity ID, Config ID).",
                "Complete all required threshold fields on each populated row.",
                "Use the Master sheets for highest parent intervals and Spread sheets for sub-periods."
        }, sectionStyle, lockedStyle);

        rowIndex = writeInstructionSection(sheet, rowIndex, "Targets", new String[]{
                "If Target Type is RELATIVE, Target must be less than or equal to 100."
        }, sectionStyle, lockedStyle);

        rowIndex = writeInstructionSection(sheet, rowIndex, "Qualifiers", new String[]{
                "Qualifier % must always be less than or equal to 100."
        }, sectionStyle, lockedStyle);

        rowIndex = writeInstructionSection(sheet, rowIndex, "Hierarchy", new String[]{
                "The Highest Parent interval MUST have an ABSOLUTE Target Type and a defined Payout.",
                "Sub-periods can have optional payouts, but each populated sub-period row must have either a Payout greater than 0 or a Qualifier % greater than 0."
        }, sectionStyle, lockedStyle);

        writeInstructionSection(sheet, rowIndex, "Caps", new String[]{
                "Max Purchase is strictly optional.",
                "Max Payout is strictly optional."
        }, sectionStyle, lockedStyle);

        sheet.setColumnWidth(0, 28000);
        sheet.protectSheet("agreement-tracker");
    }

    private int writeInstructionSection(
            Sheet sheet,
            int startRow,
            String heading,
            String[] bullets,
            CellStyle sectionStyle,
            CellStyle bodyStyle) {
        int rowIndex = startRow;
        Row headingRow = sheet.createRow(rowIndex++);
        setStringCell(headingRow, 0, heading, sectionStyle);
        for (String bullet : bullets) {
            Row bulletRow = sheet.createRow(rowIndex++);
            setStringCell(bulletRow, 0, "• " + bullet, bodyStyle);
        }
        sheet.createRow(rowIndex++);
        return rowIndex;
    }

    private SpreadSheetState createSpreadSheet(
            Workbook workbook,
            String sheetName,
            Columns layout,
            CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeHeaderRow(sheet, layout.headers(), headerStyle);
        return new SpreadSheetState(sheet, DATA_START_ROW);
    }

    private void appendMasterRows(
            MasterSheetState state,
            Columns layout,
            List<AgreementTimePeriod> parentPeriods,
            AgreementJbpConfiguration configuration,
            CellStyle lockedStyle,
            CellStyle editableStyle) {
        for (AgreementTimePeriod period : parentPeriods) {
            if (state.hasWrittenRows()) {
                state.insertGapRow();
            }
            for (int tier = 1; tier <= configuration.getSlabCount(); tier++) {
                Row dataRow = state.sheet().createRow(state.nextRow());

                setNumericCell(dataRow, layout.colEntityId(), period.getId(), lockedStyle);

                String periodDisplay = period.getName().equals(state.lastPeriodName()) ? "" : period.getName();
                setStringCell(dataRow, layout.colParentPeriod(), periodDisplay, lockedStyle);
                if (!periodDisplay.isEmpty()) {
                    state.setLastPeriodName(period.getName());
                }

                setStringCell(dataRow, layout.colSlabTier(), slabTierLabel(tier), lockedStyle);
                setNumericCell(dataRow, layout.colConfigId(), configuration.getId(), lockedStyle);
                setStringCell(dataRow, layout.colTargetType(), "ABSOLUTE", lockedStyle);

                for (int col = layout.editableStartCol(); col <= layout.editableEndCol(); col++) {
                    if (col == layout.colTargetType()) {
                        continue;
                    }
                    Cell cell = dataRow.createCell(col);
                    cell.setCellStyle(editableStyle);
                }

                state.advanceRow();
                state.markWritten();
            }
        }
    }

    private void appendSpreadRows(
            SpreadSheetState spreadState,
            Columns layout,
            List<AgreementTimePeriod> parentPeriods,
            List<AgreementTimePeriod> subPeriods,
            AgreementJbpConfiguration configuration,
            CellStyle lockedStyle,
            CellStyle editableStyle) {
        Map<Long, List<AgreementTimePeriod>> groupedSubs =
                JbpTemporalReconciliationUtil.groupSubPeriodsByParent(subPeriods, parentPeriods);

        for (AgreementTimePeriod parent : parentPeriods) {
            List<AgreementTimePeriod> subsForParent = groupedSubs.getOrDefault(parent.getId(), List.of());
            if (subsForParent.isEmpty()) {
                continue;
            }
            if (spreadState.hasWrittenRows()) {
                spreadState.insertGapRow();
            }
            int groupStartRow = spreadState.nextRow();
            for (AgreementTimePeriod subPeriod : subsForParent) {
                for (int tier = 1; tier <= configuration.getSlabCount(); tier++) {
                    Row dataRow = spreadState.sheet().createRow(spreadState.nextRow());

                    setNumericCell(dataRow, layout.colEntityId(), subPeriod.getId(), lockedStyle);

                    String parentDisplay = parent.getName().equals(spreadState.lastParentPeriod()) ? "" : parent.getName();
                    setStringCell(dataRow, layout.colParentPeriod(), parentDisplay, lockedStyle);
                    if (!parentDisplay.isEmpty()) {
                        spreadState.setLastParentPeriod(parent.getName());
                    }

                    String subDisplay = subPeriod.getName().equals(spreadState.lastSubPeriod()) ? "" : subPeriod.getName();
                    setStringCell(dataRow, layout.colSubPeriod(), subDisplay, lockedStyle);
                    if (!subDisplay.isEmpty()) {
                        spreadState.setLastSubPeriod(subPeriod.getName());
                    }

                    setStringCell(dataRow, layout.colSlabTier(), slabTierLabel(tier), lockedStyle);
                    setNumericCell(dataRow, layout.colConfigId(), configuration.getId(), lockedStyle);

                    for (int col = layout.editableStartCol(); col <= layout.editableEndCol(); col++) {
                        Cell cell = dataRow.createCell(col);
                        cell.setCellStyle(editableStyle);
                    }

                    spreadState.advanceRow();
                    spreadState.markWritten();
                }
            }
            int groupEndRow = spreadState.nextRow() - 1;
            if (groupEndRow >= groupStartRow) {
                spreadState.sheet().groupRow(groupStartRow, groupEndRow);
                spreadState.sheet().setRowSumsBelow(false);
            }
        }
    }

    private void writeHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(HEADER_ROW);
        for (int col = 0; col < headers.length; col++) {
            setStringCell(headerRow, col, headers[col], headerStyle);
        }
    }

    private void finalizeSheet(Sheet sheet, Columns layout, Workbook workbook) {
        sheet.setColumnHidden(layout.colEntityId(), true);
        sheet.setColumnHidden(layout.colConfigId(), true);
        String[] targetTypeOptions = layout.master()
                ? JbpExcelSheetLayout.ABSOLUTE_ONLY_OPTIONS
                : JbpExcelSheetLayout.VALUE_TYPE_OPTIONS;
        applyValueTypeValidation(sheet, layout.colTargetType(), workbook, targetTypeOptions);
        String[] payoutTypeOptions = layout.master()
                ? JbpExcelSheetLayout.VALUE_TYPE_OPTIONS
                : JbpExcelSheetLayout.OPTIONAL_VALUE_TYPE_OPTIONS;
        applyValueTypeValidation(sheet, layout.colPayoutType(), workbook, payoutTypeOptions);
        for (int col = 0; col < layout.headers().length; col++) {
            sheet.autoSizeColumn(col);
        }
        sheet.setRowSumsBelow(false);
        sheet.protectSheet("agreement-tracker");
    }

    private void applyValueTypeValidation(
            Sheet sheet,
            int columnIndex,
            Workbook workbook,
            String[] options) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        CellRangeAddressList addressList = new CellRangeAddressList(
                DATA_START_ROW, VALIDATION_END_ROW, columnIndex, columnIndex);
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.setSuppressDropDownArrow(true);
        sheet.addValidationData(validation);
    }

    private String slabTierLabel(int tierNumber) {
        return "Slab " + tierNumber;
    }

    private void validateContractDates(AgreementVersion version) {
        if (version.getStartDate() == null || version.getExpiryDate() == null) {
            throw new IncompleteAgreementException("Contract start and expiry dates must be saved before generating JBP workbook.");
        }
    }

    private void validateRequest(JbpWorkbookRequest request) {
        if (request.selectedFrequencies() == null || request.selectedFrequencies().isEmpty()) {
            throw new IncompleteAgreementException("Select at least one target frequency.");
        }
        if (request.configurations() == null || request.configurations().isEmpty()) {
            throw new IncompleteAgreementException("Add at least one JBP configuration.");
        }
        if (request.financialYearStartMonth() == null
                || request.financialYearStartMonth() < 1
                || request.financialYearStartMonth() > 12) {
            throw new IncompleteAgreementException("Financial year start month must be between 1 and 12.");
        }
        for (String frequency : request.selectedFrequencies()) {
            PayoutFrequency parsed = parseFrequency(frequency);
            if (!JBP_FREQUENCIES.contains(parsed)) {
                throw new BusinessException("Unsupported JBP frequency: " + frequency);
            }
        }

        for (JbpConfigurationBlockDto config : request.configurations()) {
            if (config.slabCount() == null || config.slabCount() < 1) {
                throw new BusinessException("Each configuration must define at least one slab.");
            }
            if (config.parentPeriodIds() == null || config.parentPeriodIds().isEmpty()) {
                throw new IncompleteAgreementException(
                        "Select at least one parent period for configuration " + config.configId() + ".");
            }
        }
        JbpConfigurationCollisionValidator.validateNoParentPeriodOverlap(
                request.configurations(),
                periodId -> timePeriodRepository.findById(periodId)
                        .map(AgreementTimePeriod::getName)
                        .orElse("ID " + periodId));
    }

    private List<AgreementTimePeriod> loadParentPeriods(
            AgreementVersion version,
            List<Long> parentPeriodIds,
            Integer financialYearStartMonth,
            Long currentUserId) {
        List<AgreementTimePeriod> parentPeriods = new ArrayList<>();
        PayoutFrequency expectedFrequency = null;
        for (Long periodId : parentPeriodIds) {
            AgreementTimePeriod period = timePeriodRepository.findById(periodId)
                    .orElseThrow(() -> new ResourceNotFoundException("AgreementTimePeriod", periodId));
            period = periodResolutionService.canonicalizePeriod(
                    period, version, financialYearStartMonth, currentUserId);
            if (!periodResolutionService.periodWithinContract(period, version)) {
                throw new BusinessException("Period " + period.getName() + " falls outside contract dates");
            }
            if (expectedFrequency == null) {
                expectedFrequency = period.getPeriodFrequency();
            } else if (period.getPeriodFrequency() != expectedFrequency) {
                throw new BusinessException("All parent periods in a configuration must share the same frequency.");
            }
            parentPeriods.add(period);
        }
        parentPeriods.sort(TimePeriodDimensions.chronologicalComparator());
        return parentPeriods;
    }

    private AgreementJbpConfiguration provisionJbpConfiguration(
            AgreementVersion version,
            PayoutFrequency masterFrequency,
            JbpConfigurationBlockDto config,
            List<AgreementTimePeriod> parentPeriods) {
        AgreementJbpConfiguration configuration = AgreementJbpConfiguration.builder()
                .agreementVersion(version)
                .frequency(masterFrequency)
                .slabCount(config.slabCount())
                .build();
        configuration = jbpConfigurationRepository.save(configuration);
        jbpCommercialService.persistConfigurationBlock(configuration, config, parentPeriods);
        return configuration;
    }

    private PayoutFrequency parseFrequency(String raw) {
        try {
            return PayoutFrequency.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid payout frequency: " + raw);
        }
    }

    private CellStyle createLockedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setLocked(true);
        return style;
    }

    private CellStyle createEditableStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setLocked(false);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.cloneStyleFrom(baseStyle);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private void setStringCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static final class MasterSheetState {
        private final Sheet sheet;
        private int nextRow;
        private boolean hasWrittenRows;
        private String lastPeriodName;

        private MasterSheetState(Sheet sheet, int nextRow) {
            this.sheet = sheet;
            this.nextRow = nextRow;
        }

        private Sheet sheet() {
            return sheet;
        }

        private int nextRow() {
            return nextRow;
        }

        private boolean hasWrittenRows() {
            return hasWrittenRows;
        }

        private String lastPeriodName() {
            return lastPeriodName;
        }

        private void setLastPeriodName(String lastPeriodName) {
            this.lastPeriodName = lastPeriodName;
        }

        private void insertGapRow() {
            sheet.createRow(nextRow);
            nextRow++;
            lastPeriodName = null;
        }

        private void advanceRow() {
            nextRow++;
        }

        private void markWritten() {
            hasWrittenRows = true;
        }
    }

    private static final class SpreadSheetState {
        private final Sheet sheet;
        private int nextRow;
        private boolean hasWrittenRows;
        private String lastParentPeriod;
        private String lastSubPeriod;

        private SpreadSheetState(Sheet sheet, int nextRow) {
            this.sheet = sheet;
            this.nextRow = nextRow;
        }

        private Sheet sheet() {
            return sheet;
        }

        private int nextRow() {
            return nextRow;
        }

        private boolean hasWrittenRows() {
            return hasWrittenRows;
        }

        private String lastParentPeriod() {
            return lastParentPeriod;
        }

        private void setLastParentPeriod(String lastParentPeriod) {
            this.lastParentPeriod = lastParentPeriod;
        }

        private String lastSubPeriod() {
            return lastSubPeriod;
        }

        private void setLastSubPeriod(String lastSubPeriod) {
            this.lastSubPeriod = lastSubPeriod;
        }

        private void insertGapRow() {
            sheet.createRow(nextRow);
            nextRow++;
            lastParentPeriod = null;
            lastSubPeriod = null;
        }

        private void advanceRow() {
            nextRow++;
        }

        private void markWritten() {
            hasWrittenRows = true;
        }
    }
}
