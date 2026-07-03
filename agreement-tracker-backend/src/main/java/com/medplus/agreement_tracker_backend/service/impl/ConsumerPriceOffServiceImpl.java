package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffIdsRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffRejectRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkUpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.response.ConsumerPriceOffCampaignResponse;
import com.medplus.agreement_tracker_backend.dto.request.PriceOffListFilters;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffFilterOptionDto;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffFilterOptionsResponse;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffUploadResultDto;
import com.medplus.agreement_tracker_backend.entity.ChannelMaster;
import com.medplus.agreement_tracker_backend.entity.ConsumerPriceOffCampaign;
import com.medplus.agreement_tracker_backend.entity.DiscountTypeMaster;
import com.medplus.agreement_tracker_backend.entity.ProductMaster;
import com.medplus.agreement_tracker_backend.entity.StateMaster;
import com.medplus.agreement_tracker_backend.enums.PriceOffApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;
import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.ChannelMasterRepository;
import com.medplus.agreement_tracker_backend.repository.ConsumerPriceOffCampaignRepository;
import com.medplus.agreement_tracker_backend.repository.DiscountTypeMasterRepository;
import com.medplus.agreement_tracker_backend.repository.ProductMasterRepository;
import com.medplus.agreement_tracker_backend.repository.StateMasterRepository;
import com.medplus.agreement_tracker_backend.repository.spec.PriceOffCampaignSpecifications;
import com.medplus.agreement_tracker_backend.service.ConsumerPriceOffService;
import com.medplus.agreement_tracker_backend.util.ExcelCellReader;
import com.medplus.agreement_tracker_backend.util.PriceOffCalculationEngine;
import com.medplus.agreement_tracker_backend.util.PriceOffExcelLayout;
import com.medplus.agreement_tracker_backend.util.PriceOffStatusResolver;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsumerPriceOffServiceImpl implements ConsumerPriceOffService {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    };

    private final ConsumerPriceOffCampaignRepository campaignRepository;
    private final ProductMasterRepository productRepository;
    private final StateMasterRepository stateRepository;
    private final ChannelMasterRepository channelRepository;
    private final DiscountTypeMasterRepository discountTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public PriceOffFilterOptionsResponse getFilterOptions() {
        List<PriceOffFilterOptionDto> channels = buildChannelOptions(
                channelRepository.findByIsActiveTrueOrderByChannelNameAsc())
                .stream()
                .map(name -> new PriceOffFilterOptionDto(name, name))
                .toList();
        List<PriceOffFilterOptionDto> discountTypes = discountTypeRepository
                .findByIsActiveTrueOrderByDiscountNameAsc()
                .stream()
                .map(master -> new PriceOffFilterOptionDto(
                        mapMasterToCampaignDiscountType(master).name(),
                        master.getDiscountName()))
                .toList();
        return new PriceOffFilterOptionsResponse(channels, discountTypes);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTemplate() {
        List<StateMaster> states = stateRepository.findByIsActiveTrueOrderByStateNameAsc();
        List<ChannelMaster> channels = channelRepository.findByIsActiveTrueOrderByChannelNameAsc();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(PriceOffExcelLayout.SHEET_NAME);
            Sheet listsSheet = workbook.createSheet(PriceOffExcelLayout.LISTS_SHEET);

            List<String> locationOptions = buildLocationOptions(states);
            List<String> channelOptions = buildChannelOptions(channels);
            List<String> discountOptions = Arrays.stream(PriceOffDiscountType.values())
                    .map(PriceOffDiscountType::getLabel)
                    .toList();

            writeListColumn(listsSheet, 0, locationOptions);
            writeListColumn(listsSheet, 1, channelOptions);
            writeListColumn(listsSheet, 2, discountOptions);
            workbook.setSheetHidden(workbook.getSheetIndex(listsSheet), true);

            String[] headers = {
                    PriceOffExcelLayout.COL_PRODUCT_ID,
                    PriceOffExcelLayout.COL_START_DATE,
                    PriceOffExcelLayout.COL_DURATION_MONTHS,
                    PriceOffExcelLayout.COL_MAX_UNIT_CAP,
                    PriceOffExcelLayout.COL_LOCATION,
                    PriceOffExcelLayout.COL_CHANNEL,
                    PriceOffExcelLayout.COL_DISCOUNT_TYPE,
                    PriceOffExcelLayout.COL_CP,
                    PriceOffExcelLayout.COL_MRP,
                    PriceOffExcelLayout.COL_BASE_OFFER,
                    PriceOffExcelLayout.COL_MEDPLUS_CONTRIBUTION,
                    PriceOffExcelLayout.COL_FROM_QTY,
                    PriceOffExcelLayout.COL_REMARKS,
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(PriceOffExcelLayout.HEADER_ROW);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = header.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(col);
            }

            DataValidationHelper helper = sheet.getDataValidationHelper();
            applyDateColumn(
                    workbook,
                    sheet,
                    helper,
                    PriceOffExcelLayout.COL_START_DATE_INDEX,
                    "Enter date as yyyy-MM-dd (example: 2026-07-01). Excel date picker available when cell is selected.");
            applyDropdown(helper, sheet, 4, locationOptions.size(), listsSheet.getSheetName(), 0);
            applyDropdown(helper, sheet, 5, channelOptions.size(), listsSheet.getSheetName(), 1);
            applyDropdown(helper, sheet, 6, discountOptions.size(), listsSheet.getSheetName(), 2);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Failed to generate price off template");
        }
    }

    @Override
    public PriceOffUploadResultDto uploadCampaigns(MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded Excel file is required");
        }

        List<StateMaster> allStates = stateRepository.findByIsActiveTrueOrderByStateNameAsc();
        List<ChannelMaster> allChannels = channelRepository.findByIsActiveTrueOrderByChannelNameAsc();
        Map<String, StateMaster> statesByName = allStates.stream()
                .collect(Collectors.toMap(s -> s.getStateName().toLowerCase(Locale.ROOT), s -> s, (a, b) -> a, LinkedHashMap::new));
        Map<String, ChannelMaster> channelsByName = allChannels.stream()
                .collect(Collectors.toMap(c -> c.getChannelName().toLowerCase(Locale.ROOT), c -> c, (a, b) -> a, LinkedHashMap::new));

        List<PriceOffUploadResultDto.PriceOffUploadError> errors = new ArrayList<>();
        int created = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet(PriceOffExcelLayout.SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            Map<String, Integer> headerIndex = readHeaderIndex(sheet.getRow(PriceOffExcelLayout.HEADER_ROW));
            validateRequiredHeaders(headerIndex);

            for (int rowIdx = PriceOffExcelLayout.DATA_START_ROW; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowBlank(row, headerIndex)) {
                    continue;
                }
                int displayRow = rowIdx + 1;
                try {
                    campaignRepository.save(parseRow(
                            row, headerIndex, allStates, allChannels, statesByName, channelsByName, currentUserId));
                    created++;
                } catch (BusinessException ex) {
                    errors.add(new PriceOffUploadResultDto.PriceOffUploadError(displayRow, ex.getMessage()));
                } catch (IllegalArgumentException ex) {
                    errors.add(new PriceOffUploadResultDto.PriceOffUploadError(displayRow, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new BusinessException("Failed to read uploaded price off file");
        }

        if (created == 0 && errors.isEmpty()) {
            throw new BusinessException("No campaign rows found in uploaded file");
        }
        return new PriceOffUploadResultDto(created, errors.size(), errors);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConsumerPriceOffCampaignResponse> listCampaigns(
            PriceOffListFilters filters,
            Pageable pageable,
            Long currentUserId) {
        PriceOffListFilters activeFilters = filters != null
                ? filters
                : new PriceOffListFilters(null, null, null, null, null, null);
        Specification<ConsumerPriceOffCampaign> spec = PriceOffCampaignSpecifications.combine(
                PriceOffCampaignSpecifications.withProductFilter(activeFilters.product()),
                PriceOffCampaignSpecifications.withCampaignIdFilter(activeFilters.campaignId()),
                PriceOffCampaignSpecifications.withLocationFilter(activeFilters.location()),
                PriceOffCampaignSpecifications.withChannelFilter(activeFilters.channel()),
                PriceOffCampaignSpecifications.withDiscountTypeFilter(activeFilters.discountType()),
                PriceOffCampaignSpecifications.withDisplayStatus(activeFilters.status()));
        Page<ConsumerPriceOffCampaign> result = campaignRepository.findAll(spec, pageable);
        return PagedResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumerPriceOffCampaignResponse getCampaign(Long id) {
        return toResponse(loadCampaign(id));
    }

    @Override
    public ConsumerPriceOffCampaignResponse updateCampaignId(Long id, UpdateCampaignIdRequest request, Long currentUserId) {
        ConsumerPriceOffCampaign campaign = loadCampaign(id);
        validateCampaignIdUpdateAllowed(campaign);
        applyCampaignId(campaign, normalizeCampaignId(request.campaignId()), currentUserId);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    public List<ConsumerPriceOffCampaignResponse> bulkUpdateCampaignId(
            BulkUpdateCampaignIdRequest request,
            Long currentUserId) {
        List<ConsumerPriceOffCampaign> campaigns = loadCampaigns(request.ids());
        String campaignId = normalizeCampaignId(request.campaignId());
        campaigns.forEach(campaign -> {
            validateCampaignIdUpdateAllowed(campaign);
            applyCampaignId(campaign, campaignId, currentUserId);
        });
        return campaignRepository.saveAll(campaigns).stream().map(this::toResponse).toList();
    }

    @Override
    public List<ConsumerPriceOffCampaignResponse> bulkSubmit(BulkPriceOffIdsRequest request, Long currentUserId) {
        List<ConsumerPriceOffCampaign> campaigns = loadCampaigns(request.ids());
        campaigns.forEach(campaign -> {
            if (campaign.getApprovalStatus() != PriceOffApprovalStatus.DRAFT) {
                throw new BusinessException("Only DRAFT campaigns can be submitted for approval");
            }
            campaign.setApprovalStatus(PriceOffApprovalStatus.PENDING_APPROVAL);
            campaign.setUpdatedByUserId(currentUserId);
        });
        return campaignRepository.saveAll(campaigns).stream().map(this::toResponse).toList();
    }

    @Override
    public void bulkDelete(BulkPriceOffIdsRequest request, Long currentUserId) {
        List<ConsumerPriceOffCampaign> campaigns = loadCampaigns(request.ids());
        campaigns.forEach(campaign -> {
            if (campaign.getApprovalStatus() != PriceOffApprovalStatus.DRAFT) {
                throw new BusinessException("Only DRAFT campaigns can be deleted");
            }
        });
        campaignRepository.deleteAll(campaigns);
    }

    @Override
    public List<ConsumerPriceOffCampaignResponse> bulkApprove(BulkPriceOffIdsRequest request, Long currentUserId) {
        return request.ids().stream().map(id -> approve(id, currentUserId)).toList();
    }

    @Override
    public List<ConsumerPriceOffCampaignResponse> bulkReject(BulkPriceOffRejectRequest request, Long currentUserId) {
        return request.ids().stream().map(id -> reject(id, request.remarks(), currentUserId)).toList();
    }

    @Override
    public ConsumerPriceOffCampaignResponse approve(Long id, Long currentUserId) {
        ConsumerPriceOffCampaign campaign = loadCampaign(id);
        if (campaign.getApprovalStatus() != PriceOffApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending approval campaigns can be approved");
        }
        campaign.setApprovalStatus(PriceOffApprovalStatus.APPROVED);
        campaign.setApprovedByUserId(currentUserId);
        campaign.setRejectionRemarks(null);
        campaign.setUpdatedByUserId(currentUserId);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    public ConsumerPriceOffCampaignResponse reject(Long id, String remarks, Long currentUserId) {
        if (remarks == null || remarks.isBlank()) {
            throw new BusinessException("Rejection remarks are required");
        }
        ConsumerPriceOffCampaign campaign = loadCampaign(id);
        if (campaign.getApprovalStatus() != PriceOffApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending approval campaigns can be rejected");
        }
        campaign.setApprovalStatus(PriceOffApprovalStatus.REJECTED);
        campaign.setRejectionRemarks(remarks.trim());
        campaign.setUpdatedByUserId(currentUserId);
        return toResponse(campaignRepository.save(campaign));
    }

    private ConsumerPriceOffCampaign parseRow(
            Row row,
            Map<String, Integer> headerIndex,
            List<StateMaster> allStates,
            List<ChannelMaster> allChannels,
            Map<String, StateMaster> statesByName,
            Map<String, ChannelMaster> channelsByName,
            Long currentUserId) {
        String productCode = readCell(row, headerIndex, PriceOffExcelLayout.COL_PRODUCT_ID);
        if (productCode.isBlank()) {
            throw new BusinessException("Product ID is required");
        }

        ProductMaster product = productRepository.findByProductCodeIgnoreCaseAndIsActiveTrue(productCode.trim())
                .orElseThrow(() -> new BusinessException("Product ID '" + productCode + "' not found in Product Master"));

        LocalDate startDate = parseDate(row, headerIndex, PriceOffExcelLayout.COL_START_DATE);
        if (startDate == null) {
            throw new BusinessException("Start Date is required");
        }

        Integer durationMonths = parseInteger(row, headerIndex, PriceOffExcelLayout.COL_DURATION_MONTHS);
        if (durationMonths == null || durationMonths <= 0) {
            throw new BusinessException("Duration (Months) must be a positive integer");
        }
        LocalDate endDate = startDate.plusMonths(durationMonths);

        Integer maxUnitCap = parseInteger(row, headerIndex, PriceOffExcelLayout.COL_MAX_UNIT_CAP);
        Integer fromQty = parseInteger(row, headerIndex, PriceOffExcelLayout.COL_FROM_QTY);
        String remarks = readCell(row, headerIndex, PriceOffExcelLayout.COL_REMARKS);

        String locationLabel = readCell(row, headerIndex, PriceOffExcelLayout.COL_LOCATION);
        if (locationLabel.isBlank()) {
            throw new BusinessException("Location is required");
        }
        String channelLabel = readCell(row, headerIndex, PriceOffExcelLayout.COL_CHANNEL);
        if (channelLabel.isBlank()) {
            throw new BusinessException("Channel is required");
        }

        PriceOffDiscountType discountType = PriceOffDiscountType.fromLabel(
                readCell(row, headerIndex, PriceOffExcelLayout.COL_DISCOUNT_TYPE));

        BigDecimal cp = requireDecimal(row, headerIndex, PriceOffExcelLayout.COL_CP, "CP");
        BigDecimal mrp = parseDecimal(row, headerIndex, PriceOffExcelLayout.COL_MRP);
        if (mrp == null && product.getMrp() != null) {
            mrp = product.getMrp();
        }
        if (mrp == null) {
            throw new BusinessException("MRP is required");
        }

        BigDecimal baseOffer = requireDecimal(row, headerIndex, PriceOffExcelLayout.COL_BASE_OFFER, "Base Offer");
        BigDecimal medplusContribution = requireDecimal(
                row, headerIndex, PriceOffExcelLayout.COL_MEDPLUS_CONTRIBUTION, "Medplus Contribution");

        PriceOffCalculationEngine.CalculatedFields calculated = PriceOffCalculationEngine.calculate(
                discountType, cp, mrp, baseOffer, medplusContribution);

        String l3Category = product.getL3Category() != null
                ? product.getL3Category()
                : product.getDivision().getDivisionName();

        ConsumerPriceOffCampaign campaign = ConsumerPriceOffCampaign.builder()
                .product(product)
                .productName(product.getProductName())
                .l3Category(l3Category)
                .startDate(startDate)
                .endDate(endDate)
                .durationMonths(durationMonths)
                .maxUnitCap(maxUnitCap)
                .fromQty(fromQty)
                .remarks(blankToNull(remarks))
                .locationLabel(locationLabel.trim())
                .channelLabel(channelLabel.trim())
                .discountType(discountType)
                .cp(cp)
                .mrp(mrp)
                .baseOffer(baseOffer)
                .medplusContribution(medplusContribution)
                .marginPercent(calculated.marginPercent())
                .finalOffer(calculated.finalOffer())
                .percentOff(calculated.percentOff())
                .finalMarginPercent(calculated.finalMarginPercent())
                .approvalStatus(PriceOffApprovalStatus.DRAFT)
                .unitsConsumed(0)
                .submittedByUserId(currentUserId)
                .states(resolveStates(locationLabel, allStates, statesByName))
                .channels(resolveChannels(channelLabel, allChannels, channelsByName))
                .build();
        campaign.setCreatedByUserId(currentUserId);
        campaign.setUpdatedByUserId(currentUserId);
        return campaign;
    }

    private void validateCampaignIdUpdateAllowed(ConsumerPriceOffCampaign campaign) {
        if (campaign.getApprovalStatus() != PriceOffApprovalStatus.APPROVED) {
            throw new BusinessException("Campaign ID can only be updated for approved campaigns");
        }
        PriceOffDisplayStatus displayStatus = PriceOffStatusResolver.resolve(campaign);
        if (displayStatus != PriceOffDisplayStatus.APPROVED
                && displayStatus != PriceOffDisplayStatus.PENDING_ACTIVATION) {
            throw new BusinessException("Campaign ID can only be updated for approved or pending activation campaigns");
        }
    }

    private void applyCampaignId(ConsumerPriceOffCampaign campaign, String campaignId, Long currentUserId) {
        campaign.setCampaignId(campaignId);
        campaign.setCampaignIdUpdatedAt(LocalDateTime.now());
        campaign.setCampaignIdUpdatedByUserId(currentUserId);
        campaign.setUpdatedByUserId(currentUserId);
    }

    private Set<StateMaster> resolveStates(
            String locationLabel,
            List<StateMaster> allStates,
            Map<String, StateMaster> statesByName) {
        if (PriceOffExcelLayout.PAN_INDIA.equalsIgnoreCase(locationLabel.trim())) {
            return new LinkedHashSet<>(allStates);
        }
        StateMaster state = statesByName.get(locationLabel.trim().toLowerCase(Locale.ROOT));
        if (state == null) {
            throw new BusinessException("Location '" + locationLabel + "' is not valid");
        }
        return Set.of(state);
    }

    private Set<ChannelMaster> resolveChannels(
            String channelLabel,
            List<ChannelMaster> allChannels,
            Map<String, ChannelMaster> channelsByName) {
        if (PriceOffExcelLayout.ALL_CHANNELS.equalsIgnoreCase(channelLabel.trim())) {
            return new LinkedHashSet<>(allChannels);
        }
        ChannelMaster channel = channelsByName.get(channelLabel.trim().toLowerCase(Locale.ROOT));
        if (channel == null) {
            throw new BusinessException("Channel '" + channelLabel + "' is not valid");
        }
        return Set.of(channel);
    }

    private ConsumerPriceOffCampaignResponse toResponse(ConsumerPriceOffCampaign campaign) {
        List<String> stateNames = campaign.getStates().stream().map(StateMaster::getStateName).sorted().toList();
        List<String> channelNames = campaign.getChannels().stream().map(ChannelMaster::getChannelName).sorted().toList();

        return new ConsumerPriceOffCampaignResponse(
                campaign.getId(),
                campaign.getProduct().getProductCode(),
                campaign.getProduct().getId(),
                campaign.getProductName(),
                campaign.getL3Category(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getDurationMonths(),
                campaign.getMaxUnitCap(),
                campaign.getFromQty(),
                campaign.getRemarks(),
                campaign.getCampaignId(),
                campaign.getCampaignIdUpdatedAt(),
                campaign.getCampaignIdUpdatedByUserId(),
                campaign.getLocationLabel(),
                stateNames,
                campaign.getChannelLabel(),
                channelNames,
                campaign.getDiscountType(),
                campaign.getDiscountType() != null ? campaign.getDiscountType().getLabel() : null,
                campaign.getCp(),
                campaign.getMrp(),
                campaign.getBaseOffer(),
                campaign.getMedplusContribution(),
                campaign.getMarginPercent(),
                campaign.getFinalOffer(),
                campaign.getPercentOff(),
                campaign.getFinalMarginPercent(),
                campaign.getApprovalStatus(),
                PriceOffStatusResolver.resolve(campaign),
                campaign.getUnitsConsumed(),
                campaign.getSubmittedByUserId(),
                campaign.getApprovedByUserId(),
                campaign.getRejectionRemarks(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }

    private List<ConsumerPriceOffCampaign> loadCampaigns(List<Long> ids) {
        List<ConsumerPriceOffCampaign> campaigns = campaignRepository.findByIdIn(ids);
        if (campaigns.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more price off campaigns were not found");
        }
        return campaigns;
    }

    private ConsumerPriceOffCampaign loadCampaign(Long id) {
        return campaignRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price off campaign not found: " + id));
    }

    private String normalizeCampaignId(String campaignId) {
        if (campaignId == null || campaignId.isBlank()) {
            return null;
        }
        return campaignId.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> buildLocationOptions(List<StateMaster> states) {
        List<String> options = new ArrayList<>();
        options.add(PriceOffExcelLayout.PAN_INDIA);
        states.forEach(state -> options.add(state.getStateName()));
        return options;
    }

    private List<String> buildChannelOptions(List<ChannelMaster> channels) {
        List<String> options = new ArrayList<>();
        options.add(PriceOffExcelLayout.ALL_CHANNELS);
        channels.forEach(channel -> options.add(channel.getChannelName()));
        return options;
    }

    private PriceOffDiscountType mapMasterToCampaignDiscountType(DiscountTypeMaster master) {
        return switch (master.getCalculationKind()) {
            case PERCENTAGE -> PriceOffDiscountType.DISC_PERCENT;
            case FIXED_AMOUNT -> PriceOffDiscountType.DISC_VAL;
        };
    }

    private void writeListColumn(Sheet sheet, int columnIndex, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                row = sheet.createRow(i);
            }
            row.createCell(columnIndex).setCellValue(values.get(i));
        }
    }

    private void applyDateColumn(
            Workbook workbook,
            Sheet sheet,
            DataValidationHelper helper,
            int colIndex,
            String headerComment) {
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat(PriceOffExcelLayout.EXCEL_DATE_FORMAT));
        sheet.setDefaultColumnStyle(colIndex, dateStyle);
        sheet.setColumnWidth(colIndex, 15 * 256);

        CellRangeAddressList addressList = new CellRangeAddressList(
                PriceOffExcelLayout.DATA_START_ROW,
                PriceOffExcelLayout.MAX_DATA_ROWS,
                colIndex,
                colIndex);

        DataValidationConstraint constraint = helper.createDateConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "DATE(2020,1,1)",
                "DATE(2099,12,31)",
                PriceOffExcelLayout.EXCEL_DATE_FORMAT);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowPromptBox(false);
        validation.setEmptyCellAllowed(false);
        validation.createErrorBox(
                "Invalid Date",
                "Please enter a valid date in YYYY-MM-DD format (e.g., 2026-07-01).");
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        sheet.addValidationData(validation);

        Row headerRow = sheet.getRow(PriceOffExcelLayout.HEADER_ROW);
        if (headerRow == null) {
            return;
        }
        Cell headerCell = headerRow.getCell(colIndex);
        if (headerCell == null || headerComment == null || headerComment.isBlank()) {
            return;
        }
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = createHelper.createClientAnchor();
        anchor.setCol1(colIndex);
        anchor.setCol2(colIndex + 3);
        anchor.setRow1(PriceOffExcelLayout.HEADER_ROW);
        anchor.setRow2(PriceOffExcelLayout.HEADER_ROW + 4);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(createHelper.createRichTextString(headerComment));
        headerCell.setCellComment(comment);
    }

    private void applyDropdown(
            DataValidationHelper helper,
            Sheet sheet,
            int columnIndex,
            int optionCount,
            String listsSheetName,
            int listsColumnIndex) {
        if (optionCount <= 0) {
            return;
        }
        char columnLetter = (char) ('A' + listsColumnIndex);
        String formula = listsSheetName + "!$" + columnLetter + "$1:$" + columnLetter + "$" + optionCount;
        CellRangeAddressList addressList = new CellRangeAddressList(
                PriceOffExcelLayout.DATA_START_ROW,
                PriceOffExcelLayout.MAX_DATA_ROWS,
                columnIndex,
                columnIndex);
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid value", "Select a value from the dropdown list.");
        sheet.addValidationData(validation);
    }

    private Map<String, Integer> readHeaderIndex(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException("Template header row is missing");
        }
        Map<String, Integer> index = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String label = ExcelCellReader.readAsString(cell);
            if (!label.isBlank()) {
                index.put(label, cell.getColumnIndex());
            }
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> required = List.of(
                PriceOffExcelLayout.COL_PRODUCT_ID,
                PriceOffExcelLayout.COL_START_DATE,
                PriceOffExcelLayout.COL_DURATION_MONTHS,
                PriceOffExcelLayout.COL_LOCATION,
                PriceOffExcelLayout.COL_CHANNEL,
                PriceOffExcelLayout.COL_DISCOUNT_TYPE,
                PriceOffExcelLayout.COL_CP,
                PriceOffExcelLayout.COL_MRP,
                PriceOffExcelLayout.COL_BASE_OFFER,
                PriceOffExcelLayout.COL_MEDPLUS_CONTRIBUTION);
        for (String header : required) {
            if (!headerIndex.containsKey(header)) {
                throw new BusinessException("Missing required column: " + header);
            }
        }
    }

    private boolean isRowBlank(Row row, Map<String, Integer> headerIndex) {
        return headerIndex.values().stream().allMatch(col -> ExcelCellReader.isBlank(row.getCell(col)));
    }

    private String readCell(Row row, Map<String, Integer> headerIndex, String header) {
        Integer col = headerIndex.get(header);
        if (col == null) {
            return "";
        }
        return ExcelCellReader.readAsString(row.getCell(col));
    }

    private BigDecimal parseDecimal(Row row, Map<String, Integer> headerIndex, String header) {
        Integer col = headerIndex.get(header);
        if (col == null) {
            return null;
        }
        return ExcelCellReader.readAsDecimal(row.getCell(col));
    }

    private BigDecimal requireDecimal(Row row, Map<String, Integer> headerIndex, String header, String label) {
        BigDecimal value = parseDecimal(row, headerIndex, header);
        if (value == null || value.signum() < 0) {
            throw new BusinessException(label + " is required");
        }
        return value;
    }

    private Integer parseInteger(Row row, Map<String, Integer> headerIndex, String header) {
        BigDecimal decimal = parseDecimal(row, headerIndex, header);
        return decimal == null ? null : decimal.intValue();
    }

    private LocalDate parseDate(Row row, Map<String, Integer> headerIndex, String header) {
        Integer col = headerIndex.get(header);
        if (col == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String raw = ExcelCellReader.readAsString(cell);
        if (raw.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new BusinessException("Invalid date format for Start Date: " + raw);
    }
}
