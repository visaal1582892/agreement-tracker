package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.AgreementSlabResponse;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;

import java.util.List;

public interface AgreementSlabService {

    List<AgreementSlabResponse> listSlabs(Long agreementId, CommercialSlabType slabType, Long currentUserId);

    AgreementSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId);

    AgreementSlabResponse updateSlab(Long agreementId, Long slabId, SlabDTO request, Long currentUserId);

    void deleteSlab(Long agreementId, Long slabId, Long currentUserId);
}
