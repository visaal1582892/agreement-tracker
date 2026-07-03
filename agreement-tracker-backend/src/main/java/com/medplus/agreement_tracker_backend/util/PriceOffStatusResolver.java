package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.entity.ConsumerPriceOffCampaign;
import com.medplus.agreement_tracker_backend.enums.PriceOffApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;

import java.time.LocalDate;

public final class PriceOffStatusResolver {

    private PriceOffStatusResolver() {}

    public static PriceOffDisplayStatus resolve(ConsumerPriceOffCampaign campaign) {
        return resolve(campaign, LocalDate.now());
    }

    public static PriceOffDisplayStatus resolve(ConsumerPriceOffCampaign campaign, LocalDate today) {
        PriceOffApprovalStatus approvalStatus = campaign.getApprovalStatus();
        if (approvalStatus == null) {
            return PriceOffDisplayStatus.DRAFT;
        }
        return switch (approvalStatus) {
            case DRAFT -> PriceOffDisplayStatus.DRAFT;
            case PENDING_APPROVAL -> PriceOffDisplayStatus.PENDING_APPROVAL;
            case REJECTED -> PriceOffDisplayStatus.REJECTED;
            case APPROVED -> resolveApprovedStatus(campaign, today);
        };
    }

    public static boolean isCompleted(ConsumerPriceOffCampaign campaign, LocalDate today) {
        if (campaign.getApprovalStatus() != PriceOffApprovalStatus.APPROVED) {
            return false;
        }
        if (campaign.getEndDate() != null) {
            if (today.isAfter(campaign.getEndDate())) {
                return true;
            }
        } else {
            LocalDate endDate = campaign.getStartDate().plusMonths(campaign.getDurationMonths());
            if (today.isAfter(endDate)) {
                return true;
            }
        }
        return campaign.getMaxUnitCap() != null
                && campaign.getUnitsConsumed() != null
                && campaign.getUnitsConsumed() >= campaign.getMaxUnitCap();
    }

    private static PriceOffDisplayStatus resolveApprovedStatus(
            ConsumerPriceOffCampaign campaign,
            LocalDate today) {
        if (isCompleted(campaign, today)) {
            return PriceOffDisplayStatus.COMPLETED;
        }
        if (today.isBefore(campaign.getStartDate())) {
            return PriceOffDisplayStatus.APPROVED;
        }
        if (campaign.getCampaignId() == null || campaign.getCampaignId().isBlank()) {
            return PriceOffDisplayStatus.PENDING_ACTIVATION;
        }
        return PriceOffDisplayStatus.LIVE;
    }
}
