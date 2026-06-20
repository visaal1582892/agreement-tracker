package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.AgreementSlabResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementTargetRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.service.AgreementSlabService;
import com.medplus.agreement_tracker_backend.service.CommercialVersionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementSlabServiceImpl implements AgreementSlabService {

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementSlabRepository slabRepository;
    private final AgreementTargetRepository targetRepository;
    private final CommercialVersionGuard commercialVersionGuard;

    @Override
    @Transactional(readOnly = true)
    public List<AgreementSlabResponse> listSlabs(Long agreementId, CommercialSlabType slabType, Long currentUserId) {
        loadVersionForRead(agreementId, currentUserId);
        List<AgreementSlab> slabs = slabType != null
                ? slabRepository.findByAgreementVersionIdAndSlabTypeOrderByFromValueAsc(agreementId, slabType)
                : slabRepository.findByAgreementVersionIdOrderByFromValueAsc(agreementId);
        return slabs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AgreementSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);
        CommercialSlabType slabType = resolveSlabType(request);
        validateSlabValues(request.fromValue(), request.toValue());
        validateSlabUniqueness(agreementId, request, slabType, null);

        AgreementSlab slab = AgreementSlab.builder()
                .agreementVersion(version)
                .slabType(slabType)
                .fromValue(request.fromValue())
                .toValue(request.toValue())
                .valueType(request.valueType())
                .commercialValue(request.commercialValue())
                .payoutFrequency(request.payoutFrequency())
                .build();
        slab.setCreatedByUserId(currentUserId);
        slab = slabRepository.save(slab);
        return toResponse(slab);
    }

    @Override
    @Transactional
    public AgreementSlabResponse updateSlab(Long agreementId, Long slabId, SlabDTO request, Long currentUserId) {
        commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);
        AgreementSlab slab = loadSlabForVersion(agreementId, slabId);
        CommercialSlabType slabType = resolveSlabType(request);
        validateSlabValues(request.fromValue(), request.toValue());
        validateSlabUniqueness(agreementId, request, slabType, slabId);

        slab.setSlabType(slabType);
        slab.setFromValue(request.fromValue());
        slab.setToValue(request.toValue());
        slab.setValueType(request.valueType());
        slab.setCommercialValue(request.commercialValue());
        slab.setPayoutFrequency(request.payoutFrequency());
        slab.setUpdatedByUserId(currentUserId);
        slab = slabRepository.save(slab);
        return toResponse(slab);
    }

    @Override
    @Transactional
    public void deleteSlab(Long agreementId, Long slabId, Long currentUserId) {
        commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);
        AgreementSlab slab = loadSlabForVersion(agreementId, slabId);
        targetRepository.deleteBySlabId(slab.getId());
        slabRepository.delete(slab);
    }

    private CommercialSlabType resolveSlabType(SlabDTO request) {
        return request.slabType() != null ? request.slabType() : CommercialSlabType.PURCHASE;
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

    private AgreementSlab loadSlabForVersion(Long agreementVersionId, Long slabId) {
        AgreementSlab slab = slabRepository.findById(slabId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementSlab", slabId));
        if (!slab.getAgreementVersion().getId().equals(agreementVersionId)) {
            throw new ResourceNotFoundException("AgreementSlab", slabId);
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

    private void validateSlabUniqueness(Long agreementVersionId, SlabDTO request,
                                        CommercialSlabType slabType, Long excludeSlabId) {
        List<AgreementSlab> existing = slabRepository
                .findByAgreementVersionIdAndSlabTypeOrderByFromValueAsc(agreementVersionId, slabType);
        for (AgreementSlab slab : existing) {
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

    private boolean isExactDuplicate(AgreementSlab slab, SlabDTO request) {
        return slab.getFromValue().compareTo(request.fromValue()) == 0
                && slab.getToValue().compareTo(request.toValue()) == 0
                && slab.getValueType() == request.valueType()
                && slab.getCommercialValue().compareTo(request.commercialValue()) == 0;
    }

    private boolean rangesOverlap(BigDecimal from1, BigDecimal to1, BigDecimal from2, BigDecimal to2) {
        return from1.compareTo(to2) < 0 && from2.compareTo(to1) < 0;
    }

    private String formatSlabRange(AgreementSlab slab) {
        String range = slab.getFromValue().stripTrailingZeros().toPlainString()
                + " - " + slab.getToValue().stripTrailingZeros().toPlainString();
        if (slab.getValueType().name().equals("PERCENTAGE")) {
            return range + " (" + slab.getCommercialValue().stripTrailingZeros().toPlainString() + "%)";
        }
        return range + " (Fixed: " + slab.getCommercialValue().stripTrailingZeros().toPlainString() + ")";
    }

    private AgreementSlabResponse toResponse(AgreementSlab slab) {
        return new AgreementSlabResponse(
                slab.getId(),
                slab.getSlabType(),
                slab.getFromValue(),
                slab.getToValue(),
                slab.getValueType(),
                slab.getCommercialValue(),
                slab.getPayoutFrequency());
    }
}
