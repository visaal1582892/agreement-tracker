package com.medplus.agreement_tracker_backend.dto.response.master;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductMasterResponse {
    private Long id;
    private String productCode;
    private String productName;
    private Long manufacturerId;
    private String manufacturerName;
    private Long divisionId;
    private String divisionName;
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive;
    private LocalDateTime createdAt;
    private Long createdByUserId;
    private LocalDateTime updatedAt;
    private Long updatedByUserId;
}
