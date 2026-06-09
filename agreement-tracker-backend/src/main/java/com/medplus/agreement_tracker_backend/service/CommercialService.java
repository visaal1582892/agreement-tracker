package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CommercialTemplateRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpsertSaleTargetRequest;
import com.medplus.agreement_tracker_backend.dto.response.CommercialUploadResponse;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodTargetsPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommercialService {

    byte[] generateCommercialTemplate(Long agreementId, CommercialTemplateRequest request, Long currentUserId);

    CommercialUploadResponse uploadCommercialTargets(Long agreementId, MultipartFile file, Long currentUserId);

    List<TimePeriodTargetsPreviewResponse> getTargetsPreview(Long agreementId, Long currentUserId);

    void upsertSaleTarget(Long agreementId, UpsertSaleTargetRequest request, Long currentUserId);
}
