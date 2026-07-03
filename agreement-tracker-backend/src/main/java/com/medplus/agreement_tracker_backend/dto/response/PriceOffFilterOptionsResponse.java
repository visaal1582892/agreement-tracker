package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record PriceOffFilterOptionsResponse(
        List<PriceOffFilterOptionDto> channels,
        List<PriceOffFilterOptionDto> discountTypes
) {}
