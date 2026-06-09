package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.PurchaseSlabResponse;
import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementPurchaseSlab;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementPurchaseSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSaleTargetRepository;
import com.medplus.agreement_tracker_backend.service.AgreementPurchaseSlabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementPurchaseSlabServiceImpl implements AgreementPurchaseSlabService {

    private final AgreementRepository agreementRepository;
    private final AgreementPurchaseSlabRepository purchaseSlabRepository;
    private final AgreementSaleTargetRepository saleTargetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseSlabResponse> listSlabs(Long agreementId, Long currentUserId) {
        loadAgreementForRead(agreementId, currentUserId);
        return purchaseSlabRepository.findByAgreementIdOrderByFromValueAsc(agreementId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PurchaseSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId) {
        Agreement agreement = loadDraftAgreementForMutation(agreementId, currentUserId);
        validateSlabValues(request.fromValue(), request.toValue());

        AgreementPurchaseSlab slab = AgreementPurchaseSlab.builder()
                .agreement(agreement)
                .fromValue(request.fromValue())
                .toValue(request.toValue())
                .valueType(request.valueType())
                .commercialValue(request.commercialValue())
                .build();
        slab.setCreatedByUserId(currentUserId);
        slab = purchaseSlabRepository.save(slab);
        return toResponse(slab);
    }

    @Override
    @Transactional
    public PurchaseSlabResponse updateSlab(Long agreementId, Long slabId, SlabDTO request, Long currentUserId) {
        loadDraftAgreementForMutation(agreementId, currentUserId);
        AgreementPurchaseSlab slab = loadSlabForAgreement(agreementId, slabId);
        validateSlabValues(request.fromValue(), request.toValue());

        slab.setFromValue(request.fromValue());
        slab.setToValue(request.toValue());
        slab.setValueType(request.valueType());
        slab.setCommercialValue(request.commercialValue());
        slab.setUpdatedByUserId(currentUserId);
        slab = purchaseSlabRepository.save(slab);
        return toResponse(slab);
    }

    @Override
    @Transactional
    public void deleteSlab(Long agreementId, Long slabId, Long currentUserId) {
        loadDraftAgreementForMutation(agreementId, currentUserId);
        AgreementPurchaseSlab slab = loadSlabForAgreement(agreementId, slabId);
        saleTargetRepository.deleteBySlabId(slab.getId());
        purchaseSlabRepository.delete(slab);
    }

    private Agreement loadDraftAgreementForMutation(Long agreementId, Long userId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        if (!agreement.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        if (agreement.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new UnauthorizedException(
                    "Purchase slabs can only be modified while the agreement is in DRAFT status");
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

    private AgreementPurchaseSlab loadSlabForAgreement(Long agreementId, Long slabId) {
        AgreementPurchaseSlab slab = purchaseSlabRepository.findById(slabId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseSlab", slabId));
        if (!slab.getAgreement().getId().equals(agreementId)) {
            throw new ResourceNotFoundException("PurchaseSlab", slabId);
        }
        return slab;
    }

    private void validateSlabValues(BigDecimal fromValue, BigDecimal toValue) {
        if (fromValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("fromValue must be greater than or equal to 0");
        }
        if (toValue.compareTo(fromValue) <= 0) {
            throw new BusinessException("toValue must be greater than fromValue");
        }
    }

    private PurchaseSlabResponse toResponse(AgreementPurchaseSlab slab) {
        return new PurchaseSlabResponse(
                slab.getId(),
                slab.getFromValue(),
                slab.getToValue(),
                slab.getValueType(),
                slab.getCommercialValue());
    }
}
