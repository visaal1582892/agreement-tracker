package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto;
import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto.StagedSheet;
import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto.UnpivotedRow;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpConfiguration;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.enums.JbpValueType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ExcelValidationException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpConfigurationRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpVersionFrequencyRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import com.medplus.agreement_tracker_backend.service.JbpExcelParserService;
import com.medplus.agreement_tracker_backend.util.ExcelCellReader;
import com.medplus.agreement_tracker_backend.util.JbpExcelUserMessages;
import com.medplus.agreement_tracker_backend.util.JbpExcelRowErrorCollector;
import com.medplus.agreement_tracker_backend.util.JbpExcelSheetLayout;
import com.medplus.agreement_tracker_backend.util.JbpExcelSheetLayout.Columns;
import com.medplus.agreement_tracker_backend.util.JbpExcelValidationErrorAnnotator;
import com.medplus.agreement_tracker_backend.util.JbpTemporalReconciliationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JbpExcelParserServiceImpl implements JbpExcelParserService {

    private static final int HEADER_ROW = 0;
    private static final int DATA_START_ROW = 1;
    private static final BigDecimal PERCENT_LIMIT = new BigDecimal("100");

    private static final Pattern SHEET_PATTERN = Pattern.compile("^(Master|Spread)_([A-Z_]+)$");

    private final CommercialVersionGuard commercialVersionGuard;
    private final AgreementTimePeriodRepository timePeriodRepository;
    private final AgreementJbpConfigurationRepository jbpConfigurationRepository;
    private final AgreementJbpVersionFrequencyRepository versionFrequencyRepository;

    @Override
    @Transactional(readOnly = true)
    public JbpStagedWorkbookDto parseUpload(Long agreementVersionId, MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new IncompleteAgreementException("Uploaded Excel file is required.");
        }

        commercialVersionGuard.loadForCommercialMutation(agreementVersionId, currentUserId);
        List<String> selectedFrequencies = versionFrequencyRepository
                .findByAgreementVersionIdOrderByFrequencyAsc(agreementVersionId).stream()
                .map(frequency -> frequency.getFrequency().name())
                .toList();
        ensureStoredConfigurations(agreementVersionId);
        Map<Long, AgreementJbpConfiguration> configurationsById =
                jbpConfigurationRepository.findByAgreementVersionId(agreementVersionId).stream()
                        .collect(Collectors.toMap(AgreementJbpConfiguration::getId, Function.identity()));

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException ex) {
            throw new IncompleteAgreementException("Failed to read JBP workbook: " + ex.getMessage());
        }

        try {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IncompleteAgreementException("Excel file does not contain any worksheets.");
            }

            JbpExcelRowErrorCollector rowErrors = new JbpExcelRowErrorCollector();
            List<ParsedSheet> parsedSheets = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                ParsedSheet parsed = parseSheet(sheet, configurationsById, rowErrors);
                if (parsed != null) {
                    parsedSheets.add(parsed);
                }
            }

            if (parsedSheets.isEmpty() && !rowErrors.hasErrors()) {
                throw new IncompleteAgreementException("No recognizable JBP worksheets found.");
            }

            for (ParsedSheet sheet : parsedSheets) {
                validateIncreasingTargets(sheet, rowErrors);
            }

            if (rowErrors.hasErrors()) {
                byte[] errorWorkbook = JbpExcelValidationErrorAnnotator.annotateWorkbook(
                        workbook, rowErrors.rowErrors());
                throw new ExcelValidationException(errorWorkbook);
            }

            List<StagedSheet> stagedSheets = parsedSheets.stream()
                    .sorted(Comparator
                            .comparing(ParsedSheet::master).reversed()
                            .thenComparing(sheet -> JbpTemporalReconciliationUtil.frequencyRank(sheet.frequency()),
                                    Comparator.reverseOrder()))
                    .map(this::toStagedSheet)
                    .toList();

            return new JbpStagedWorkbookDto(stagedSheets, selectedFrequencies, false, null);
        } finally {
            try {
                workbook.close();
            } catch (IOException ex) {
                throw new IncompleteAgreementException("Failed to close JBP workbook: " + ex.getMessage());
            }
        }
    }

    private void ensureStoredConfigurations(Long agreementVersionId) {
        if (jbpConfigurationRepository.findByAgreementVersionId(agreementVersionId).isEmpty()) {
            throw new IncompleteAgreementException(
                    "Generate the JBP workbook template before uploading a completed file.");
        }
    }

    private ParsedSheet parseSheet(
            Sheet sheet,
            Map<Long, AgreementJbpConfiguration> configurationsById,
            JbpExcelRowErrorCollector rowErrors) {
        SheetIdentity identity = parseSheetIdentity(sheet.getSheetName());
        if (identity == null) {
            return null;
        }

        Columns layout = JbpExcelSheetLayout.forSheet(identity.master());
        String sheetName = sheet.getSheetName();

        if (sheet.getRow(HEADER_ROW) == null) {
            throw new IncompleteAgreementException(
                    "Invalid template format on sheet '" + sheetName + "': missing header row.");
        }

        List<IndexedRow> rows = new ArrayList<>();
        String lastParentPeriod = null;
        String lastSubPeriod = null;
        for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isGapRow(row, layout)) {
                continue;
            }
            try {
                IndexedRow parsed = identity.master()
                        ? parseMasterRow(row, sheetName, identity, layout, configurationsById, lastParentPeriod)
                        : parseSpreadRow(row, sheetName, identity, layout, configurationsById,
                        lastParentPeriod, lastSubPeriod);
                if (parsed == null) {
                    continue;
                }
                IndexedRow normalized = normalizeQualifierDefault(parsed);
                enforceMandatoryFields(normalized);
                validateIndexedRow(normalized, identity.master(), sheetName, rowErrors);
                if (rowErrors.rowErrors().getOrDefault(sheetName, Map.of()).containsKey(normalized.excelRowIndex())) {
                    continue;
                }
                rows.add(normalized);
                lastParentPeriod = normalized.row().parentPeriodName();
                if (normalized.row().subPeriodName() != null) {
                    lastSubPeriod = normalized.row().subPeriodName();
                }
            } catch (Exception ex) {
                rowErrors.add(sheetName, rowIndex, resolveErrorMessage(ex));
            }
        }

        if (rows.isEmpty() && !rowErrors.rowErrors().containsKey(sheetName)) {
            throw new IncompleteAgreementException("No data rows found on sheet '" + sheetName + "'.");
        }

        applyFirstInParentGroupFlags(rows);
        return new ParsedSheet(sheetName, identity.frequency(), identity.master(), rows);
    }

    private SheetIdentity parseSheetIdentity(String sheetName) {
        Matcher matcher = SHEET_PATTERN.matcher(sheetName);
        if (!matcher.matches()) {
            return null;
        }
        boolean master = "Master".equals(matcher.group(1));
        PayoutFrequency frequency;
        try {
            frequency = PayoutFrequency.valueOf(matcher.group(2));
        } catch (IllegalArgumentException ex) {
            throw new IncompleteAgreementException("Unrecognized JBP frequency in sheet name: '" + sheetName + "'.");
        }
        return new SheetIdentity(master, frequency);
    }

    private boolean isGapRow(Row row, Columns layout) {
        Long entityId = readEntityId(row.getCell(layout.colEntityId()));
        Long configId = readEntityId(row.getCell(layout.colConfigId()));
        return entityId == null && configId == null;
    }

    private IndexedRow parseMasterRow(
            Row row,
            String sheetName,
            SheetIdentity identity,
            Columns layout,
            Map<Long, AgreementJbpConfiguration> configurationsById,
            String lastParentPeriod) {
        int excelRowIndex = row.getRowNum();
        Long entityId = readEntityId(row.getCell(layout.colEntityId()));
        Long configurationId = readEntityId(row.getCell(layout.colConfigId()));
        if (entityId == null || configurationId == null) {
            return null;
        }

        String periodNameRaw = ExcelCellReader.readAsString(row.getCell(layout.colParentPeriod()));
        String periodName = periodNameRaw.isBlank() ? lastParentPeriod : periodNameRaw;
        if (periodName == null || periodName.isBlank()) {
            return null;
        }

        String tierLabel = ExcelCellReader.readAsString(row.getCell(layout.colSlabTier()));
        int tierNumber = parseTierNumber(tierLabel);

        AgreementTimePeriod period = resolvePeriod(entityId, periodName, identity.frequency(), sheetName);
        resolveConfiguration(configurationId, tierNumber, configurationsById);

        ThresholdValues values = readThresholdValues(row, layout, true);
        boolean firstInGroup = !periodName.equals(lastParentPeriod);

        return new IndexedRow(
                new UnpivotedRow(
                        periodName,
                        period.getId(),
                        null,
                        period.getId(),
                        tierNumber,
                        configurationId,
                        tierLabel,
                        values.targetType(),
                        values.target(),
                        values.qualifierPercent(),
                        values.payoutType(),
                        values.payout(),
                        values.maxPurchase(),
                        values.maxPayout(),
                        firstInGroup),
                excelRowIndex);
    }

    private IndexedRow parseSpreadRow(
            Row row,
            String sheetName,
            SheetIdentity identity,
            Columns layout,
            Map<Long, AgreementJbpConfiguration> configurationsById,
            String lastParentPeriod,
            String lastSubPeriod) {
        int excelRowIndex = row.getRowNum();
        Long entityId = readEntityId(row.getCell(layout.colEntityId()));
        Long configurationId = readEntityId(row.getCell(layout.colConfigId()));
        if (entityId == null || configurationId == null) {
            return null;
        }

        String parentRaw = ExcelCellReader.readAsString(row.getCell(layout.colParentPeriod()));
        String parentPeriodName = parentRaw.isBlank() ? lastParentPeriod : parentRaw;
        String subRaw = ExcelCellReader.readAsString(row.getCell(layout.colSubPeriod()));
        String subPeriodName = subRaw.isBlank() ? lastSubPeriod : subRaw;
        if (parentPeriodName == null || parentPeriodName.isBlank()
                || subPeriodName == null || subPeriodName.isBlank()) {
            return null;
        }

        String tierLabel = ExcelCellReader.readAsString(row.getCell(layout.colSlabTier()));
        int tierNumber = parseTierNumber(tierLabel);

        AgreementTimePeriod subPeriod = resolvePeriod(entityId, subPeriodName, identity.frequency(), sheetName);
        AgreementTimePeriod parentPeriod = timePeriodRepository.findByName(parentPeriodName)
                .orElseThrow(() -> new IncompleteAgreementException(
                        JbpExcelUserMessages.unknownParentPeriod(parentPeriodName)));
        resolveConfiguration(configurationId, tierNumber, configurationsById);

        ThresholdValues values = readThresholdValues(row, layout, false);
        boolean firstInGroup = !parentPeriodName.equals(lastParentPeriod);

        return new IndexedRow(
                new UnpivotedRow(
                        parentPeriodName,
                        parentPeriod.getId(),
                        subPeriodName,
                        subPeriod.getId(),
                        tierNumber,
                        configurationId,
                        tierLabel,
                        values.targetType(),
                        values.target(),
                        values.qualifierPercent(),
                        values.payoutType(),
                        values.payout(),
                        values.maxPurchase(),
                        values.maxPayout(),
                        firstInGroup),
                excelRowIndex);
    }

    private IndexedRow normalizeQualifierDefault(IndexedRow indexedRow) {
        UnpivotedRow row = indexedRow.row();
        if (row.qualifierPercent() != null) {
            return indexedRow;
        }
        return new IndexedRow(copyRow(row, row.firstInParentGroup(), BigDecimal.ZERO), indexedRow.excelRowIndex());
    }

    private void enforceMandatoryFields(IndexedRow indexedRow) {
        UnpivotedRow row = indexedRow.row();
        if (row.targetType() == null) {
            throw new BusinessException(JbpExcelUserMessages.targetTypeMandatory());
        }
        if (row.target() == null) {
            throw new BusinessException(JbpExcelUserMessages.targetValueMandatory());
        }
    }

    private void validateIndexedRow(
            IndexedRow indexedRow,
            boolean masterSheet,
            String sheetName,
            JbpExcelRowErrorCollector rowErrors) {
        UnpivotedRow row = indexedRow.row();
        int excelRowIndex = indexedRow.excelRowIndex();

        if (masterSheet) {
            if (row.targetType() != null && row.targetType() == JbpValueType.RELATIVE) {
                rowErrors.add(sheetName, excelRowIndex, JbpExcelUserMessages.parentTargetMustBeAbsolute());
            }
            if (row.payoutType() == null || row.payout() == null) {
                rowErrors.add(sheetName, excelRowIndex, JbpExcelUserMessages.parentPayoutRequired());
            }
        } else {
            validateSubPeriodSignificance(row, sheetName, excelRowIndex, rowErrors);
        }

        if (row.qualifierPercent().compareTo(PERCENT_LIMIT) > 0) {
            rowErrors.add(sheetName, excelRowIndex,
                    JbpExcelUserMessages.qualifierTooHigh(row.slabTierLabel()));
        }
        if (row.targetType() == JbpValueType.RELATIVE
                && row.target() != null
                && row.target().compareTo(PERCENT_LIMIT) > 0) {
            rowErrors.add(sheetName, excelRowIndex,
                    JbpExcelUserMessages.relativeTargetTooHigh(row.slabTierLabel()));
        }
        if (row.payoutType() == JbpValueType.RELATIVE
                && row.payout() != null
                && row.payout().compareTo(PERCENT_LIMIT) > 0) {
            rowErrors.add(sheetName, excelRowIndex,
                    JbpExcelUserMessages.relativePayoutTooHigh(row.slabTierLabel()));
        }
    }

    private void validateSubPeriodSignificance(
            UnpivotedRow row,
            String sheetName,
            int excelRowIndex,
            JbpExcelRowErrorCollector rowErrors) {
        boolean hasPayout = row.payoutType() != null
                && row.payout() != null
                && row.payout().compareTo(BigDecimal.ZERO) > 0;
        boolean hasQualifier = row.qualifierPercent().compareTo(BigDecimal.ZERO) > 0;
        if (hasPayout || hasQualifier) {
            return;
        }
        rowErrors.add(sheetName, excelRowIndex, JbpExcelUserMessages.subPeriodNeedsPayoutOrQualifier());
    }

    private AgreementTimePeriod resolvePeriod(
            Long entityId,
            String periodName,
            PayoutFrequency frequency,
            String sheetName) {
        AgreementTimePeriod period = timePeriodRepository.findById(entityId)
                .orElseThrow(() -> new IncompleteAgreementException(
                        JbpExcelUserMessages.unknownEntityId(entityId)));
        if (!period.getName().equals(periodName)) {
            throw new IncompleteAgreementException(
                    JbpExcelUserMessages.entityPeriodMismatch(entityId, periodName));
        }
        if (period.getPeriodFrequency() != frequency) {
            throw new IncompleteAgreementException(
                    JbpExcelUserMessages.wrongPeriodFrequency(periodName, frequency));
        }
        return period;
    }

    private AgreementJbpConfiguration resolveConfiguration(
            Long configurationId,
            int tierNumber,
            Map<Long, AgreementJbpConfiguration> configurationsById) {
        AgreementJbpConfiguration configuration = configurationsById.get(configurationId);
        if (configuration == null) {
            throw new IncompleteAgreementException(
                    JbpExcelUserMessages.invalidConfigurationId(configurationId));
        }
        if (configuration.getSlabCount() == null || tierNumber < 1 || tierNumber > configuration.getSlabCount()) {
            throw new IncompleteAgreementException(
                    JbpExcelUserMessages.invalidSlabForConfiguration(configurationId, tierNumber));
        }
        return configuration;
    }

    private void applyFirstInParentGroupFlags(List<IndexedRow> rows) {
        String lastParent = null;
        for (int i = 0; i < rows.size(); i++) {
            IndexedRow indexedRow = rows.get(i);
            UnpivotedRow row = indexedRow.row();
            boolean first = !row.parentPeriodName().equals(lastParent);
            if (first != row.firstInParentGroup()) {
                rows.set(i, new IndexedRow(copyRow(row, first), indexedRow.excelRowIndex()));
            }
            lastParent = row.parentPeriodName();
        }
    }

    private UnpivotedRow copyRow(UnpivotedRow row, boolean firstInGroup) {
        return copyRow(row, firstInGroup, row.qualifierPercent());
    }

    private UnpivotedRow copyRow(UnpivotedRow row, boolean firstInGroup, BigDecimal qualifierPercent) {
        BigDecimal qualifier = qualifierPercent != null ? qualifierPercent : BigDecimal.ZERO;
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
                qualifier,
                row.payoutType(),
                row.payout(),
                row.maxPurchase(),
                row.maxPayout(),
                firstInGroup);
    }

    private ThresholdValues readThresholdValues(Row row, Columns layout, boolean masterSheet) {
        BigDecimal qualifier = readLenientDecimal(row.getCell(layout.colQualifierPercent()));
        if (qualifier == null) {
            qualifier = BigDecimal.ZERO;
        }
        return new ThresholdValues(
                parseValueType(row.getCell(layout.colTargetType()), "Target Type"),
                readStrictDecimal(row.getCell(layout.colTarget()), "Target"),
                qualifier,
                parseOptionalValueType(row.getCell(layout.colPayoutType()), "Payout Type", masterSheet),
                readLenientDecimal(row.getCell(layout.colPayout())),
                readLenientDecimal(row.getCell(layout.colMaxPurchase())),
                readLenientDecimal(row.getCell(layout.colMaxPayout())));
    }

    private JbpValueType parseOptionalValueType(Cell cell, String label, boolean masterSheet) {
        if (masterSheet) {
            return parseValueType(cell, label);
        }
        return parseValueType(cell, label, true);
    }

    private JbpValueType parseValueType(Cell cell, String label) {
        return parseValueType(cell, label, false);
    }

    private JbpValueType parseValueType(Cell cell, String label, boolean allowBlank) {
        String raw = ExcelCellReader.readAsString(cell);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return JbpValueType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IncompleteAgreementException(JbpExcelUserMessages.invalidValueType(label));
        }
    }

    private int parseTierNumber(String tierLabel) {
        String digits = tierLabel.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IncompleteAgreementException(JbpExcelUserMessages.invalidSlabTier(tierLabel));
        }
        return Integer.parseInt(digits);
    }

    private Long readEntityId(Cell cell) {
        BigDecimal value = ExcelCellReader.readAsFormattedDecimal(cell);
        return value == null ? null : value.longValue();
    }

    private void validateIncreasingTargets(ParsedSheet sheet, JbpExcelRowErrorCollector rowErrors) {
        Map<String, List<IndexedRow>> rowsByGroup = new LinkedHashMap<>();
        for (IndexedRow indexedRow : sheet.rows()) {
            UnpivotedRow row = indexedRow.row();
            if (row.target() == null) {
                continue;
            }
            String groupKey = row.timePeriodId() + "::" + row.jbpConfigurationId();
            rowsByGroup.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(indexedRow);
        }

        for (List<IndexedRow> groupRows : rowsByGroup.values()) {
            List<IndexedRow> sorted = groupRows.stream()
                    .sorted(Comparator.comparing(indexedRow -> indexedRow.row().slabTierNumber()))
                    .toList();
            for (int index = 1; index < sorted.size(); index++) {
                UnpivotedRow previous = sorted.get(index - 1).row();
                IndexedRow currentIndexed = sorted.get(index);
                UnpivotedRow current = currentIndexed.row();
                if (current.target().compareTo(previous.target()) <= 0) {
                    rowErrors.add(
                            sheet.sheetName(),
                            currentIndexed.excelRowIndex(),
                            JbpExcelUserMessages.targetMustIncrease(
                                    current.slabTierLabel(),
                                    previous.slabTierLabel(),
                                    previous.target().toPlainString(),
                                    current.target().toPlainString()));
                }
            }
        }
    }

    private BigDecimal readStrictDecimal(Cell cell, String fieldLabel) {
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) {
            return null;
        }
        if (ExcelCellReader.hasNonBlankNonNumericContent(cell)) {
            throw new IncompleteAgreementException(JbpExcelUserMessages.invalidNumberFormat(fieldLabel));
        }
        return ExcelCellReader.readAsFormattedDecimal(cell);
    }

    private BigDecimal readLenientDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK) {
            return null;
        }
        if (ExcelCellReader.hasNonBlankNonNumericContent(cell)) {
            throw new IncompleteAgreementException(JbpExcelUserMessages.invalidRowFormat());
        }
        return ExcelCellReader.readAsFormattedDecimal(cell);
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex instanceof IncompleteAgreementException || ex instanceof BusinessException) {
            return JbpExcelUserMessages.normalizeCaughtMessage(ex.getMessage());
        }
        return JbpExcelUserMessages.invalidRowFormat();
    }

    private StagedSheet toStagedSheet(ParsedSheet sheet) {
        return new StagedSheet(
                null,
                sheet.sheetName(),
                sheet.sheetName(),
                sheet.frequency().name(),
                sheet.master(),
                sheet.rows().stream().map(IndexedRow::row).toList());
    }

    private record ThresholdValues(
            JbpValueType targetType,
            BigDecimal target,
            BigDecimal qualifierPercent,
            JbpValueType payoutType,
            BigDecimal payout,
            BigDecimal maxPurchase,
            BigDecimal maxPayout) {}

    private record SheetIdentity(boolean master, PayoutFrequency frequency) {}

    private record IndexedRow(UnpivotedRow row, int excelRowIndex) {}

    private record ParsedSheet(
            String sheetName,
            PayoutFrequency frequency,
            boolean master,
            List<IndexedRow> rows) {}
}
