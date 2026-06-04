package com.medplus.agreement_tracker_backend.dto.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MasterPageRequest {
    private int page = 0;
    private int size = 20;
    private String sortBy = "id";
    private String sortDirection = "ASC";
    private Map<String, String> filters = new HashMap<>();
}
