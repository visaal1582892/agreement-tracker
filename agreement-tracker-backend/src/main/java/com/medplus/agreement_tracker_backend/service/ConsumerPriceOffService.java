package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffIdsRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffRejectRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkUpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.response.ConsumerPriceOffCampaignResponse;
import com.medplus.agreement_tracker_backend.dto.request.PriceOffListFilters;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffFilterOptionsResponse;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffUploadResultDto;
import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConsumerPriceOffService {

    byte[] generateTemplate();

    PriceOffUploadResultDto uploadCampaigns(MultipartFile file, Long currentUserId);

    PriceOffFilterOptionsResponse getFilterOptions();

    PagedResponse<ConsumerPriceOffCampaignResponse> listCampaigns(
            PriceOffListFilters filters,
            Pageable pageable,
            Long currentUserId);

    ConsumerPriceOffCampaignResponse getCampaign(Long id);

    ConsumerPriceOffCampaignResponse updateCampaignId(Long id, UpdateCampaignIdRequest request, Long currentUserId);

    List<ConsumerPriceOffCampaignResponse> bulkUpdateCampaignId(
            BulkUpdateCampaignIdRequest request,
            Long currentUserId);

    List<ConsumerPriceOffCampaignResponse> bulkSubmit(BulkPriceOffIdsRequest request, Long currentUserId);

    void bulkDelete(BulkPriceOffIdsRequest request, Long currentUserId);

    List<ConsumerPriceOffCampaignResponse> bulkApprove(BulkPriceOffIdsRequest request, Long currentUserId);

    List<ConsumerPriceOffCampaignResponse> bulkReject(BulkPriceOffRejectRequest request, Long currentUserId);

    ConsumerPriceOffCampaignResponse approve(Long id, Long currentUserId);

    ConsumerPriceOffCampaignResponse reject(Long id, String remarks, Long currentUserId);
}
