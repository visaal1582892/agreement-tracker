package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.constants.IncomeTypeNames;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.IncomeType;
import com.medplus.agreement_tracker_backend.enums.AdHocSubType;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.ConflictValidationException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CommercialVersionGuardTest {

    @InjectMocks
    private CommercialVersionGuard commercialVersionGuard;

    private AgreementVersion approvedVersion;
    private AgreementVersion qpsDraftVersion;

    @BeforeEach
    void setUp() {
        approvedVersion = AgreementVersion.builder()
                .id(1L)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        IncomeType adHocType = IncomeType.builder()
                .id(10L)
                .name(IncomeTypeNames.AD_HOC_ACTIVITIES)
                .build();

        qpsDraftVersion = AgreementVersion.builder()
                .id(2L)
                .approvalStatus(ApprovalStatus.DRAFT)
                .incomeType(adHocType)
                .adhocSubType(AdHocSubType.QPS)
                .build();
    }

    @Test
    void assertApprovedCommercialImmutability_rejectsApprovedVersion() {
        ConflictValidationException ex = assertThrows(
                ConflictValidationException.class,
                () -> commercialVersionGuard.assertApprovedCommercialImmutability(approvedVersion));

        assertEquals(
                "Cannot modify commercial terms of an approved agreement directly. "
                        + "A new draft version must be created first.",
                ex.getMessage());
    }

    @Test
    void validateQpsPayoutFrequencies_rejectsRecurringFrequencies() {
        IncompleteAgreementException ex = assertThrows(
                IncompleteAgreementException.class,
                () -> commercialVersionGuard.validateQpsPayoutFrequencies(
                        qpsDraftVersion,
                        List.of("MONTHLY")));

        assertEquals(
                "Security Violation: QPS agreements cannot have recurring payout frequencies.",
                ex.getMessage());
    }

    @Test
    void validateQpsPayoutFrequencies_allowsOneTime() {
        assertDoesNotThrow(() -> commercialVersionGuard.validateQpsPayoutFrequencies(
                qpsDraftVersion,
                List.of("ONE_TIME")));
    }
}
