package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgreementStatusResolver {

    public AgreementStatus resolve(Agreement agreement) {
        if (agreement.getTerminationDate() != null) {
            return AgreementStatus.TERMINATED;
        }
        if (agreement.isInProgressFlag()) {
            return AgreementStatus.IN_PROGRESS;
        }
        if (agreement.getApprovalStatus() == ApprovalStatus.DRAFT) {
            return AgreementStatus.DRAFT;
        }
        if (agreement.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL) {
            return AgreementStatus.PENDING_APPROVAL;
        }
        if (agreement.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return AgreementStatus.REJECTED;
        }
        if (agreement.getApprovalStatus() == ApprovalStatus.APPROVED) {
            Long currentVersionId = agreement.getAgreementGroup().getCurrentVersionId();
            if (currentVersionId != null && !currentVersionId.equals(agreement.getId())) {
                return AgreementStatus.SUPERSEDED;
            }
            if (agreement.getExpiryDate() != null && agreement.getExpiryDate().isBefore(LocalDate.now())) {
                return AgreementStatus.EXPIRED;
            }
            return AgreementStatus.ACTIVE;
        }
        return AgreementStatus.DRAFT;
    }
}
