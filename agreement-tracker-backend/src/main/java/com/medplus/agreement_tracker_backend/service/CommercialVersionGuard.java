package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.constants.IncomeTypeNames;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.AdHocSubType;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.ConflictValidationException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommercialVersionGuard {

    private static final String APPROVED_MUTATION_MSG =
            "Cannot modify commercial terms of an approved agreement directly. "
                    + "A new draft version must be created first.";

    private static final String QPS_FREQUENCY_MSG =
            "Security Violation: QPS agreements cannot have recurring payout frequencies.";

    private final AgreementVersionRepository agreementVersionRepository;

    public AgreementVersion loadForCommercialMutation(Long agreementVersionId, Long userId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        assertApprovedCommercialImmutability(version);

        if (!version.getAgreement().getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new UnauthorizedException(
                    "Commercial terms can only be modified while the agreement is in DRAFT status");
        }
        return version;
    }

    public void assertApprovedCommercialImmutability(AgreementVersion version) {
        if (version.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new ConflictValidationException(APPROVED_MUTATION_MSG);
        }
    }

    public void validateQpsPayoutFrequencies(AgreementVersion version, List<String> selectedFrequencies) {
        if (!isQpsAgreement(version)) {
            return;
        }
        if (selectedFrequencies == null
                || selectedFrequencies.size() != 1
                || !"ONE_TIME".equals(selectedFrequencies.get(0))) {
            throw new IncompleteAgreementException(QPS_FREQUENCY_MSG);
        }
    }

    private boolean isQpsAgreement(AgreementVersion version) {
        if (version.getIncomeType() == null
                || !IncomeTypeNames.AD_HOC_ACTIVITIES.equalsIgnoreCase(version.getIncomeType().getName())) {
            return false;
        }
        return version.getAdhocSubType() == AdHocSubType.QPS;
    }
}
