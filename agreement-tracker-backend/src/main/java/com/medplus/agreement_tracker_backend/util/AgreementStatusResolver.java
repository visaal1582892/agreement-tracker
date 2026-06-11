package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgreementStatusResolver {

    public AgreementStatus resolve(AgreementVersion version) {
        if (version.getTerminationDate() != null) {
            return AgreementStatus.TERMINATED;
        }
        if (version.isInProgressFlag()) {
            return AgreementStatus.IN_PROGRESS;
        }
        if (version.getApprovalStatus() == ApprovalStatus.DRAFT) {
            return AgreementStatus.DRAFT;
        }
        if (version.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL) {
            return AgreementStatus.PENDING_APPROVAL;
        }
        if (version.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return AgreementStatus.REJECTED;
        }
        if (version.getApprovalStatus() == ApprovalStatus.APPROVED) {
            Long currentVersionId = version.getAgreement().getCurrentVersionId();
            if (currentVersionId != null && !currentVersionId.equals(version.getId())) {
                return AgreementStatus.SUPERSEDED;
            }
            if (version.getExpiryDate() != null && version.getExpiryDate().isBefore(LocalDate.now())) {
                return AgreementStatus.EXPIRED;
            }
            return AgreementStatus.ACTIVE;
        }
        return AgreementStatus.DRAFT;
    }
}
