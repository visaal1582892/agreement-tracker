package com.medplus.agreement_tracker_backend.dto.response.master;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Builder
public class StateMasterResponse {
    private Long id;
    private String stateName;
    private String stateCode;
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive;
    private LocalDateTime createdAt;
    private Long createdByUserId;
    private LocalDateTime updatedAt;
    private Long updatedByUserId;
}
