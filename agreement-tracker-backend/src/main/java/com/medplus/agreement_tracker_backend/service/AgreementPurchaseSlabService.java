package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.PurchaseSlabResponse;

import java.util.List;

public interface AgreementPurchaseSlabService {

    List<PurchaseSlabResponse> listSlabs(Long agreementId, Long currentUserId);

    PurchaseSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId);

    PurchaseSlabResponse updateSlab(Long agreementId, Long slabId, SlabDTO request, Long currentUserId);

    void deleteSlab(Long agreementId, Long slabId, Long currentUserId);
}
