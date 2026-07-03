package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.response.AgreementStoreMappingResponse;
import com.medplus.agreement_tracker_backend.dto.response.StoreUploadResultDto;
import com.medplus.agreement_tracker_backend.entity.AgreementStoreMapping;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.StateMaster;
import com.medplus.agreement_tracker_backend.entity.StoreMaster;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementStoreMappingRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.repository.StoreMasterRepository;
import com.medplus.agreement_tracker_backend.service.StoreMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreMappingServiceImpl implements StoreMappingService {

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementStoreMappingRepository mappingRepository;
    private final StoreMasterRepository storeMasterRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Store Codes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Store Code");
            sheet.autoSizeColumn(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("Failed to generate store mapping template");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementStoreMappingResponse> listMappings(Long agreementVersionId, Long currentUserId) {
        AgreementVersion version = loadDraftVersion(agreementVersionId, currentUserId);
        return toResponses(mappingRepository.findByAgreementVersionIdOrderByStoreStoreCodeAsc(version.getId()));
    }

    @Override
    public StoreUploadResultDto uploadMappings(
            Long agreementVersionId,
            MultipartFile file,
            Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded Excel file is required");
        }

        AgreementVersion version = loadDraftVersion(agreementVersionId, currentUserId);
        Set<Long> scopedStateIds = version.getAgreement().getStates().stream()
                .map(StateMaster::getId)
                .collect(Collectors.toSet());

        List<String> storeCodes = parseStoreCodes(file);
        if (storeCodes.isEmpty()) {
            throw new BusinessException("No store codes found in uploaded file");
        }

        List<StoreUploadResultDto.StoreUploadError> skippedStores = new ArrayList<>();
        List<StoreMaster> validStoresToMap = new ArrayList<>();
        Set<Long> seenStoreIds = new LinkedHashSet<>();

        for (String code : storeCodes) {
            StoreMaster store = storeMasterRepository
                    .findByStoreCodeIgnoreCaseAndIsActiveTrue(code.trim())
                    .orElse(null);
            if (store == null) {
                skippedStores.add(new StoreUploadResultDto.StoreUploadError(
                        code,
                        "Store ID not found in Master database"));
                continue;
            }

            StateMaster storeState = store.getState();
            if (!scopedStateIds.contains(storeState.getId())) {
                skippedStores.add(new StoreUploadResultDto.StoreUploadError(
                        code,
                        "Store belongs to '" + storeState.getStateName()
                                + "' which is outside the Step 2 Geography scope"));
                continue;
            }

            if (seenStoreIds.add(store.getId())) {
                validStoresToMap.add(store);
            }
        }

        List<AgreementStoreMapping> mappingsToSave = new ArrayList<>();
        for (StoreMaster store : validStoresToMap) {
            if (!mappingRepository.existsByAgreementVersionIdAndStoreId(version.getId(), store.getId())) {
                mappingsToSave.add(AgreementStoreMapping.builder()
                        .agreementVersion(version)
                        .store(store)
                        .build());
            }
        }
        if (!mappingsToSave.isEmpty()) {
            mappingRepository.saveAll(mappingsToSave);
        }

        List<AgreementStoreMappingResponse> successfullyMapped = validStoresToMap.stream()
                .map(store -> mappingRepository
                        .findByAgreementVersionIdAndStoreId(version.getId(), store.getId())
                        .map(this::toResponse)
                        .orElse(null))
                .filter(response -> response != null)
                .toList();

        StoreUploadResultDto result = new StoreUploadResultDto();
        result.setSuccessfullyMapped(new ArrayList<>(successfullyMapped));
        result.setSkippedStores(skippedStores);
        result.setTotalAttempted(storeCodes.size());
        return result;
    }

    @Override
    public void deleteMappings(Long agreementVersionId, List<Long> mappingIds, Long currentUserId) {
        AgreementVersion version = loadDraftVersion(agreementVersionId, currentUserId);
        mappingRepository.deleteByAgreementVersionIdAndIdIn(version.getId(), mappingIds);
    }

    @Override
    public void copyMappings(Long sourceVersionId, Long targetVersionId) {
        AgreementVersion target = agreementVersionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", targetVersionId));
        mappingRepository.deleteByAgreementVersionId(targetVersionId);

        List<AgreementStoreMapping> sourceMappings =
                mappingRepository.findByAgreementVersionIdOrderByStoreStoreCodeAsc(sourceVersionId);
        for (AgreementStoreMapping sourceMapping : sourceMappings) {
            mappingRepository.save(AgreementStoreMapping.builder()
                    .agreementVersion(target)
                    .store(sourceMapping.getStore())
                    .build());
        }
    }

    private AgreementVersion loadDraftVersion(Long agreementVersionId, Long currentUserId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        if (!version.getAgreement().getOwner().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Store mappings can only be modified on draft versions");
        }
        return version;
    }

    private List<String> parseStoreCodes(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel file does not contain any worksheets");
            }
            Set<String> codes = new LinkedHashSet<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || row.getCell(0) == null) {
                    continue;
                }
                String code = readCellAsString(row.getCell(0));
                if (!code.isBlank()) {
                    codes.add(code.trim());
                }
            }
            return new ArrayList<>(codes);
        } catch (IOException ex) {
            throw new BusinessException("Failed to parse uploaded Excel file");
        }
    }

    private String readCellAsString(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private AgreementStoreMappingResponse toResponse(AgreementStoreMapping mapping) {
        return new AgreementStoreMappingResponse(
                mapping.getId(),
                mapping.getStore().getId(),
                mapping.getStore().getStoreCode(),
                mapping.getStore().getStoreName(),
                mapping.getStore().getState().getId(),
                mapping.getStore().getState().getStateName());
    }

    private List<AgreementStoreMappingResponse> toResponses(List<AgreementStoreMapping> mappings) {
        return mappings.stream()
                .map(this::toResponse)
                .toList();
    }
}
