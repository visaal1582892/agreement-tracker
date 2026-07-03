package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.AgreementSlabResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.CapUnit;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementSlabRepository;
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
    private final CommercialVersionGuard commercialVersionGuard;

    @Override
    @Transactional(readOnly = true)
    public List<AgreementSlabResponse> listSlabs(Long agreementId, CommercialSlabType slabType, Long currentUserId) {
        loadVersionForRead(agreementId, currentUserId);
        List<AgreementSlab> slabs = slabType != null
                ? slabRepository.findByAgreementVersionIdAndSlabTypeOrderByMinCapAsc(agreementId, slabType)
                : slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementId);
        return slabs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AgreementSlabResponse createSlab(Long agreementId, SlabDTO request, Long currentUserId) {
        AgreementVersion version = commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);
        CommercialSlabType slabType = resolveSlabType(request);
        validateSlabValues(request.minCap(), request.maxCap());
        CapUnit capUnit = resolveCapUnit(agreementId, request.capUnit());
        validateSlabUniqueness(agreementId, request, slabType, capUnit, null);

        AgreementSlab slab = AgreementSlab.builder()
                .agreementVersion(version)
                .slabType(slabType)
                .minCap(request.minCap())
                .maxCap(request.maxCap())
                .capUnit(capUnit)
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
        validateSlabValues(request.minCap(), request.maxCap());
        CapUnit capUnit = resolveCapUnit(agreementId, request.capUnit());
        validateSlabUniqueness(agreementId, request, slabType, capUnit, slabId);

        slab.setSlabType(slabType);
        slab.setMinCap(request.minCap());
        slab.setMaxCap(request.maxCap());
        slab.setCapUnit(capUnit);
        slab.setValueType(request.valueType());
        slab.setCommercialValue(request.commercialValue());
        slab.setPayoutFrequency(request.payoutFrequency());
        slab.setUpdatedByUserId(currentUserId);
        slab = slabRepository.save(slab);
        syncCapUnitAcrossVersion(agreementId, capUnit);
        return toResponse(slab);
    }

    @Override
    @Transactional
    public void deleteSlab(Long agreementId, Long slabId, Long currentUserId) {
        commercialVersionGuard.loadForCommercialMutation(agreementId, currentUserId);
        AgreementSlab slab = loadSlabForVersion(agreementId, slabId);
        slabRepository.delete(slab);
    }

    private CommercialSlabType resolveSlabType(SlabDTO request) {
        return request.slabType() != null ? request.slabType() : CommercialSlabType.PURCHASE;
    }

    private CapUnit resolveCapUnit(Long agreementVersionId, CapUnit requested) {
        List<AgreementSlab> existing = slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementVersionId);
        if (existing.isEmpty()) {
            return requested != null ? requested : CapUnit.RUPEES;
        }
        CapUnit versionUnit = existing.get(0).getCapUnit();
        if (requested != null && requested != versionUnit) {
            throw new BusinessException("Cap unit must match the agreement slab table unit (" + versionUnit + ")");
        }
        return versionUnit;
    }

    private void syncCapUnitAcrossVersion(Long agreementVersionId, CapUnit capUnit) {
        List<AgreementSlab> slabs = slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementVersionId);
        for (AgreementSlab slab : slabs) {
            if (slab.getCapUnit() != capUnit) {
                slab.setCapUnit(capUnit);
                slabRepository.save(slab);
            }
        }
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

    private void validateSlabValues(BigDecimal minCap, BigDecimal maxCap) {
        if (minCap.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("minCap must be greater than or equal to 0");
        }
        if (maxCap.compareTo(minCap) <= 0) {
            throw new BusinessException("Max Cap must be strictly greater than Min Cap");
        }
    }

    private void validateSlabUniqueness(Long agreementVersionId, SlabDTO request,
                                        CommercialSlabType slabType, CapUnit capUnit, Long excludeSlabId) {
        List<AgreementSlab> existing = slabRepository
                .findByAgreementVersionIdAndSlabTypeOrderByMinCapAsc(agreementVersionId, slabType);
        for (AgreementSlab slab : existing) {
            if (excludeSlabId != null && slab.getId().equals(excludeSlabId)) {
                continue;
            }
            if (isExactDuplicate(slab, request, capUnit)) {
                throw new BusinessException("This slab rule already exists");
            }
            if (rangesOverlap(slab.getMinCap(), slab.getMaxCap(), request.minCap(), request.maxCap())) {
                throw new BusinessException(
                        "Slab range overlaps with an existing slab (" + formatSlabRange(slab) + ")");
            }
        }
    }

    private boolean isExactDuplicate(AgreementSlab slab, SlabDTO request, CapUnit capUnit) {
        return slab.getMinCap().compareTo(request.minCap()) == 0
                && slab.getMaxCap().compareTo(request.maxCap()) == 0
                && slab.getCapUnit() == capUnit
                && slab.getValueType() == request.valueType()
                && slab.getCommercialValue().compareTo(request.commercialValue()) == 0;
    }

    private boolean rangesOverlap(BigDecimal from1, BigDecimal to1, BigDecimal from2, BigDecimal to2) {
        return from1.compareTo(to2) < 0 && from2.compareTo(to1) < 0;
    }

    private String formatSlabRange(AgreementSlab slab) {
        String range = slab.getMinCap().stripTrailingZeros().toPlainString()
                + " - " + slab.getMaxCap().stripTrailingZeros().toPlainString();
        if (slab.getValueType().name().equals("PERCENTAGE")) {
            return range + " (" + slab.getCommercialValue().stripTrailingZeros().toPlainString() + "%)";
        }
        return range + " (Fixed: " + slab.getCommercialValue().stripTrailingZeros().toPlainString() + ")";
    }

    private AgreementSlabResponse toResponse(AgreementSlab slab) {
        return new AgreementSlabResponse(
                slab.getId(),
                slab.getSlabType(),
                slab.getMinCap(),
                slab.getMaxCap(),
                slab.getCapUnit(),
                slab.getValueType(),
                slab.getCommercialValue(),
                slab.getPayoutFrequency());
    }
}
