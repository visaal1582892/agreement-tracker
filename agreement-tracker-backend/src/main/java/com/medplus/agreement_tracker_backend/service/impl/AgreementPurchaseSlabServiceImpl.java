package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.PurchaseSlabResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementPurchaseSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementPurchaseSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSaleTargetRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.service.AgreementPurchaseSlabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementPurchaseSlabServiceImpl implements AgreementPurchaseSlabService {

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementPurchaseSlabRepository purchaseSlabRepository;
    private final AgreementSaleTargetRepository saleTargetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseSlabResponse> listSlabs(Long agreementId, Long currentUserId) {
        loadVersionForRead(agreementId, currentUserId);
        return purchaseSlabRepository.findByAgreementVersionIdOrderByFromValueAsc(agreementId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PurchaseSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId) {
        AgreementVersion version = loadDraftVersionForMutation(agreementId, currentUserId);
        validateSlabValues(request.fromValue(), request.toValue());
        validateSlabUniqueness(agreementId, request, null);

        AgreementPurchaseSlab slab = AgreementPurchaseSlab.builder()
                .agreementVersion(version)
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
        loadDraftVersionForMutation(agreementId, currentUserId);
        AgreementPurchaseSlab slab = loadSlabForVersion(agreementId, slabId);
        validateSlabValues(request.fromValue(), request.toValue());
        validateSlabUniqueness(agreementId, request, slabId);

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
        loadDraftVersionForMutation(agreementId, currentUserId);
        AgreementPurchaseSlab slab = loadSlabForVersion(agreementId, slabId);
        saleTargetRepository.deleteBySlabId(slab.getId());
        purchaseSlabRepository.delete(slab);
    }

    private AgreementVersion loadDraftVersionForMutation(Long agreementVersionId, Long userId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        if (!version.getAgreement().getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new UnauthorizedException(
                    "Purchase slabs can only be modified while the agreement is in DRAFT status");
        }
        return version;
    }

    private AgreementVersion loadVersionForRead(Long agreementVersionId, Long userId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        if (version.getApprovalStatus() == ApprovalStatus.DRAFT
                && !version.getAgreement().getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("AgreementVersion", agreementVersionId);
        }
        return version;
    }

    private AgreementPurchaseSlab loadSlabForVersion(Long agreementVersionId, Long slabId) {
        AgreementPurchaseSlab slab = purchaseSlabRepository.findById(slabId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseSlab", slabId));
        if (!slab.getAgreementVersion().getId().equals(agreementVersionId)) {
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

    private void validateSlabUniqueness(Long agreementVersionId, SlabDTO request, Long excludeSlabId) {
        List<AgreementPurchaseSlab> existing =
                purchaseSlabRepository.findByAgreementVersionIdOrderByFromValueAsc(agreementVersionId);
        for (AgreementPurchaseSlab slab : existing) {
            if (excludeSlabId != null && slab.getId().equals(excludeSlabId)) {
                continue;
            }
            if (isExactDuplicate(slab, request)) {
                throw new BusinessException("This slab rule already exists");
            }
            if (rangesOverlap(slab.getFromValue(), slab.getToValue(), request.fromValue(), request.toValue())) {
                throw new BusinessException(
                        "Slab range overlaps with an existing slab (" + formatSlabRange(slab) + ")");
            }
        }
    }

    private boolean isExactDuplicate(AgreementPurchaseSlab slab, SlabDTO request) {
        return slab.getFromValue().compareTo(request.fromValue()) == 0
                && slab.getToValue().compareTo(request.toValue()) == 0
                && slab.getValueType() == request.valueType()
                && slab.getCommercialValue().compareTo(request.commercialValue()) == 0;
    }

    private boolean rangesOverlap(BigDecimal from1, BigDecimal to1, BigDecimal from2, BigDecimal to2) {
        return from1.compareTo(to2) < 0 && from2.compareTo(to1) < 0;
    }

    private String formatSlabRange(AgreementPurchaseSlab slab) {
        String range = slab.getFromValue().stripTrailingZeros().toPlainString()
                + " - " + slab.getToValue().stripTrailingZeros().toPlainString();
        if (slab.getValueType().name().equals("PERCENTAGE")) {
            return range + " (" + slab.getCommercialValue().stripTrailingZeros().toPlainString() + "%)";
        }
        return range + " (Fixed: " + slab.getCommercialValue().stripTrailingZeros().toPlainString() + ")";
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
