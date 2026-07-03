package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.response.AgreementStoreMappingResponse;
import com.medplus.agreement_tracker_backend.dto.response.StoreUploadResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StoreMappingService {

    byte[] generateTemplate();

    List<AgreementStoreMappingResponse> listMappings(Long agreementVersionId, Long currentUserId);

    StoreUploadResultDto uploadMappings(Long agreementVersionId, MultipartFile file, Long currentUserId);

    void deleteMappings(Long agreementVersionId, List<Long> mappingIds, Long currentUserId);

    void copyMappings(Long sourceVersionId, Long targetVersionId);
}
