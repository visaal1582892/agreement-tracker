package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CommercialTemplateRequest;
import com.medplus.agreement_tracker_backend.dto.request.CommercialTypeSwitchRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpsertTargetRequest;
import com.medplus.agreement_tracker_backend.dto.response.CommercialUploadResponse;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodTargetsPreviewResponse;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommercialService {

    byte[] generateCommercialTemplate(Long agreementId, CommercialTemplateRequest request, Long currentUserId);

    CommercialUploadResponse uploadCommercialTargets(
            Long agreementId, MultipartFile file, CommercialSlabType slabType, Long currentUserId);

    List<TimePeriodTargetsPreviewResponse> getTargetsPreview(Long agreementId, Long currentUserId);

    void upsertTarget(Long agreementId, UpsertTargetRequest request, Long currentUserId);

    void switchCommercialType(Long agreementId, CommercialTypeSwitchRequest request, Long currentUserId);
}
