package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CommercialTemplateRequest;
import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.request.UpsertSaleTargetRequest;
import com.medplus.agreement_tracker_backend.dto.response.CommercialUploadResponse;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodTargetsPreviewResponse;
import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementPurchaseSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementSaleTarget;
import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementPurchaseSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSaleTargetRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTimePeriodRepository;
import com.medplus.agreement_tracker_backend.service.CommercialService;
import com.medplus.agreement_tracker_backend.util.ExcelCellReader;
import com.medplus.agreement_tracker_backend.util.SlabHeaderFormatter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
public class CommercialServiceImpl implements CommercialService {

    private static final Set<String> VALID_FREQUENCIES =
            Set.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY");

    private static final DateTimeFormatter MONTHLY_FORMAT =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private static final Map<String, BiFunction<LocalDate, LocalDate, List<String>>> PERIOD_GENERATORS = Map.of(
            "MONTHLY", CommercialServiceImpl::generateMonthlyPeriods,
            "QUARTERLY", CommercialServiceImpl::generateQuarterlyPeriods,
            "HALF_YEARLY", CommercialServiceImpl::generateHalfYearlyPeriods,
            "YEARLY", CommercialServiceImpl::generateYearlyPeriods
    );

    private final AgreementRepository agreementRepository;
    private final AgreementPurchaseSlabRepository purchaseSlabRepository;
    private final AgreementSaleTargetRepository saleTargetRepository;
    private final AgreementTimePeriodRepository timePeriodRepository;

    @Override
    @Transactional
    public byte[] generateCommercialTemplate(Long agreementId, CommercialTemplateRequest request, Long currentUserId) {
        Agreement agreement = loadDraftAgreementForMutation(agreementId, currentUserId);

        validateAgreementDates(agreement);
        validateFrequencies(request.selectedFrequencies());

        List<AgreementPurchaseSlab> purchaseSlabs =
                purchaseSlabRepository.findByAgreementIdOrderByFromValueAsc(agreementId);
        if (purchaseSlabs.isEmpty()) {
            throw new IncompleteAgreementException(
                    "No purchase slabs found for this agreement. Add at least one slab before generating the template.");
        }

        LocalDate startDate = agreement.getStartDate();
        LocalDate expiryDate = agreement.getExpiryDate();
        List<String> groupedPeriodNames = buildGroupedPeriodNames(
                request.selectedFrequencies(), startDate, expiryDate, currentUserId);

        List<SlabDTO> slabDtos = purchaseSlabs.stream()
                .map(slab -> new SlabDTO(
                        slab.getFromValue(),
                        slab.getToValue(),
                        slab.getValueType(),
                        slab.getCommercialValue()))
                .toList();

        return buildExcel(slabDtos, groupedPeriodNames);
    }

    @Override
    @Transactional
    public CommercialUploadResponse uploadCommercialTargets(Long agreementId, MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new IncompleteAgreementException("Uploaded Excel file is required.");
        }

        Agreement agreement = loadDraftAgreementForMutation(agreementId, currentUserId);

        List<AgreementPurchaseSlab> slabs = purchaseSlabRepository.findByAgreementIdOrderByFromValueAsc(agreementId);
        if (slabs.isEmpty()) {
            throw new IncompleteAgreementException(
                    "No purchase slabs found for this agreement. Generate the commercial template first.");
        }

        saleTargetRepository.deleteByAgreementId(agreementId);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IncompleteAgreementException("Excel file does not contain any worksheets.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IncompleteAgreementException("Excel file is missing the header row.");
            }

            Map<Integer, AgreementPurchaseSlab> columnSlabs = mapHeaderColumns(headerRow, slabs);
            if (columnSlabs.isEmpty()) {
                throw new IncompleteAgreementException(
                        "No slab columns matched the saved purchase slabs for this agreement.");
            }

            int savedCount = 0;
            int skippedRows = 0;
            int skippedCells = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || ExcelCellReader.isBlank(row.getCell(0))) {
                    continue;
                }

                String periodName = ExcelCellReader.readAsString(row.getCell(0));
                AgreementTimePeriod timePeriod = timePeriodRepository.findByName(periodName).orElse(null);
                if (timePeriod == null) {
                    skippedRows++;
                    continue;
                }

                boolean rowHasSavedTarget = false;
                for (Map.Entry<Integer, AgreementPurchaseSlab> entry : columnSlabs.entrySet()) {
                    BigDecimal targetValue = ExcelCellReader.readAsDecimal(row.getCell(entry.getKey()));
                    if (targetValue == null) {
                        skippedCells++;
                        continue;
                    }

                    AgreementSaleTarget target = AgreementSaleTarget.builder()
                            .agreement(agreement)
                            .timePeriod(timePeriod)
                            .slab(entry.getValue())
                            .targetValue(targetValue)
                            .build();
                    target.setCreatedByUserId(currentUserId);
                    saleTargetRepository.save(target);
                    savedCount++;
                    rowHasSavedTarget = true;
                }

                if (!rowHasSavedTarget) {
                    skippedRows++;
                }
            }

            return new CommercialUploadResponse(savedCount, skippedRows, skippedCells);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded commercial Excel file", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimePeriodTargetsPreviewResponse> getTargetsPreview(Long agreementId, Long currentUserId) {
        loadAgreementForRead(agreementId, currentUserId);

        List<AgreementSaleTarget> targets = saleTargetRepository.findByAgreementId(agreementId);
        Map<Long, String> periodNames = new HashMap<>();
        Map<Long, Map<Long, BigDecimal>> periodTargets = new HashMap<>();

        for (AgreementSaleTarget target : targets) {
            Long periodId = target.getTimePeriod().getId();
            periodNames.put(periodId, target.getTimePeriod().getName());
            periodTargets.computeIfAbsent(periodId, id -> new HashMap<>())
                    .put(target.getSlab().getId(), target.getTargetValue());
        }

        return periodNames.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> new TimePeriodTargetsPreviewResponse(
                        entry.getKey(),
                        entry.getValue(),
                        Map.copyOf(periodTargets.get(entry.getKey()))))
                .toList();
    }

    @Override
    @Transactional
    public void upsertSaleTarget(Long agreementId, UpsertSaleTargetRequest request, Long currentUserId) {
        Agreement agreement = loadDraftAgreementForMutation(agreementId, currentUserId);

        AgreementTimePeriod timePeriod = timePeriodRepository.findById(request.timePeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("TimePeriod", request.timePeriodId()));

        AgreementPurchaseSlab slab = purchaseSlabRepository.findById(request.slabId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseSlab", request.slabId()));
        if (!slab.getAgreement().getId().equals(agreementId)) {
            throw new ResourceNotFoundException("PurchaseSlab", request.slabId());
        }

        Optional<AgreementSaleTarget> existing = saleTargetRepository
                .findByAgreementIdAndTimePeriodIdAndSlabId(agreementId, request.timePeriodId(), request.slabId());

        if (request.targetValue() == null) {
            existing.ifPresent(saleTargetRepository::delete);
            return;
        }

        if (existing.isPresent()) {
            AgreementSaleTarget target = existing.get();
            target.setTargetValue(request.targetValue());
            target.setUpdatedByUserId(currentUserId);
            saleTargetRepository.save(target);
            return;
        }

        AgreementSaleTarget target = AgreementSaleTarget.builder()
                .agreement(agreement)
                .timePeriod(timePeriod)
                .slab(slab)
                .targetValue(request.targetValue())
                .build();
        target.setCreatedByUserId(currentUserId);
        saleTargetRepository.save(target);
    }

    private Agreement loadDraftAgreementForMutation(Long agreementId, Long userId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        if (!agreement.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        if (agreement.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new UnauthorizedException(
                    "Commercial targets can only be modified while the agreement is in DRAFT status");
        }
        return agreement;
    }

    private Agreement loadAgreementForRead(Long agreementId, Long userId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        if (agreement.getApprovalStatus() == ApprovalStatus.DRAFT
                && !agreement.getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("Agreement", agreementId);
        }
        return agreement;
    }

    private Map<Integer, AgreementPurchaseSlab> mapHeaderColumns(Row headerRow, List<AgreementPurchaseSlab> slabs) {
        Map<String, AgreementPurchaseSlab> headerLookup = new HashMap<>();
        for (AgreementPurchaseSlab slab : slabs) {
            headerLookup.put(SlabHeaderFormatter.format(slab), slab);
        }

        Map<Integer, AgreementPurchaseSlab> columnSlabs = new HashMap<>();
        for (int columnIndex = 1; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            Cell headerCell = headerRow.getCell(columnIndex);
            String header = ExcelCellReader.readAsString(headerCell);
            if (header.isEmpty()) {
                continue;
            }
            AgreementPurchaseSlab slab = headerLookup.get(header);
            if (slab != null) {
                columnSlabs.put(columnIndex, slab);
            }
        }
        return columnSlabs;
    }

    AgreementTimePeriod getOrCreateTimePeriod(String name, Long userId) {
        return timePeriodRepository.findByName(name)
                .orElseGet(() -> createTimePeriod(name, userId));
    }

    private AgreementTimePeriod createTimePeriod(String name, Long userId) {
        AgreementTimePeriod period = AgreementTimePeriod.builder()
                .name(name)
                .build();
        period.setCreatedByUserId(userId);
        try {
            return timePeriodRepository.save(period);
        } catch (DataIntegrityViolationException ex) {
            return timePeriodRepository.findByName(name)
                    .orElseThrow(() -> ex);
        }
    }

    private void validateAgreementDates(Agreement agreement) {
        if (agreement.getStartDate() == null || agreement.getExpiryDate() == null) {
            throw new IncompleteAgreementException(
                    "Agreement start date and expiry date are required to generate a commercial template.");
        }
        if (agreement.getExpiryDate().isBefore(agreement.getStartDate())) {
            throw new IncompleteAgreementException(
                    "Agreement expiry date must be on or after the start date.");
        }
    }

    private void validateFrequencies(List<String> selectedFrequencies) {
        for (String frequency : selectedFrequencies) {
            if (!VALID_FREQUENCIES.contains(frequency)) {
                throw new IncompleteAgreementException(
                        "Invalid frequency: " + frequency + ". Allowed values: MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY.");
            }
        }
    }

    private List<String> buildGroupedPeriodNames(
            List<String> selectedFrequencies,
            LocalDate startDate,
            LocalDate expiryDate,
            Long userId) {
        List<String> groupedNames = new ArrayList<>();

        for (int i = 0; i < selectedFrequencies.size(); i++) {
            String frequency = selectedFrequencies.get(i);
            List<String> periodNames = PERIOD_GENERATORS.get(frequency).apply(startDate, expiryDate);

            for (String name : periodNames) {
                getOrCreateTimePeriod(name, userId);
                groupedNames.add(name);
            }

            if (i < selectedFrequencies.size() - 1) {
                groupedNames.add(null);
            }
        }

        return groupedNames;
    }

    private static List<String> generateMonthlyPeriods(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        YearMonth current = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);

        while (!current.isAfter(last)) {
            periods.add(current.format(MONTHLY_FORMAT));
            current = current.plusMonths(1);
        }
        return periods;
    }

    private static List<String> generateQuarterlyPeriods(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        int year = start.getYear();
        int quarter = quarterOf(start);
        int endYear = end.getYear();
        int endQuarter = quarterOf(end);

        while (year < endYear || (year == endYear && quarter <= endQuarter)) {
            periods.add("Q" + quarter + " " + year);
            quarter++;
            if (quarter > 4) {
                quarter = 1;
                year++;
            }
        }
        return periods;
    }

    private static List<String> generateHalfYearlyPeriods(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        int year = start.getYear();
        int half = halfOf(start);
        int endYear = end.getYear();
        int endHalf = halfOf(end);

        while (year < endYear || (year == endYear && half <= endHalf)) {
            periods.add("H" + half + " " + year);
            if (half == 1) {
                half = 2;
            } else {
                half = 1;
                year++;
            }
        }
        return periods;
    }

    private static List<String> generateYearlyPeriods(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            periods.add(String.valueOf(year));
        }
        return periods;
    }

    private static int quarterOf(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    private static int halfOf(LocalDate date) {
        return date.getMonthValue() <= 6 ? 1 : 2;
    }

    private byte[] buildExcel(List<SlabDTO> slabs, List<String> groupedPeriodNames) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Commercial Template");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Time Period");
            for (int i = 0; i < slabs.size(); i++) {
                headerRow.createCell(i + 1).setCellValue(SlabHeaderFormatter.format(slabs.get(i)));
            }

            int rowIndex = 1;
            for (String periodName : groupedPeriodNames) {
                if (periodName == null) {
                    rowIndex++;
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(periodName);
            }

            for (int i = 0; i <= slabs.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate commercial Excel template", ex);
        }
    }
}
