package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CommitCutoffsRequest;
import com.medplus.agreement_tracker_backend.dto.response.SlabPeriodCutoffMatrixResponse;
import com.medplus.agreement_tracker_backend.dto.response.StagedCutoffMatrixResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CommercialContactsService {

    byte[] generateCutoffTemplate(Long agreementVersionId, Long currentUserId);

    StagedCutoffMatrixResponse parseCutoffUpload(Long agreementVersionId, MultipartFile file, Long currentUserId);

    void commitCutoffs(Long agreementVersionId, CommitCutoffsRequest request, Long currentUserId);

    SlabPeriodCutoffMatrixResponse listCutoffMatrix(Long agreementVersionId, Long currentUserId);

    void purgeCommercialStructureData(Long agreementVersionId, Long currentUserId);
}
