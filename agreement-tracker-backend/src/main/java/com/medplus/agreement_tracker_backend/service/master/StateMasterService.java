package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.StateMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.StateMasterResponse;
import com.medplus.agreement_tracker_backend.entity.StateMaster;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.StateMasterRepository;
import com.medplus.agreement_tracker_backend.util.SpecificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StateMasterService {

    private final StateMasterRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<StateMasterResponse> search(MasterPageRequest req) {
        Specification<StateMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<StateMaster> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public StateMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<StateMasterResponse> findAllActive() {
        return repository.findByIsActiveTrueOrderByStateNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public StateMasterResponse create(StateMasterRequest req, Long userId) {
        String code = req.getStateCode().trim().toUpperCase();
        if (repository.existsByStateCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("State code already exists: " + code);
        }
        StateMaster entity = StateMaster.builder()
                .stateName(req.getStateName().trim())
                .stateCode(code)
                .build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public StateMasterResponse update(Long id, StateMasterRequest req, Long userId) {
        StateMaster entity = findOrThrow(id);
        String code = req.getStateCode().trim().toUpperCase();
        if (!code.equalsIgnoreCase(entity.getStateCode()) && repository.existsByStateCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("State code already exists: " + code);
        }
        entity.setStateName(req.getStateName().trim());
        entity.setStateCode(code);
        if (req.getIsActive() != null) {
            entity.setActive(req.getIsActive());
        }
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        StateMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private StateMaster findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("StateMaster", id));
    }

    private Specification<StateMaster> buildSpec(Map<String, String> filters) {
        Specification<StateMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) {
            return spec;
        }
        if (filters.containsKey("stateName")) {
            spec = spec.and(SpecificationUtils.stringLike("stateName", filters.get("stateName")));
        }
        if (filters.containsKey("stateCode")) {
            spec = spec.and(SpecificationUtils.stringLike("stateCode", filters.get("stateCode")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private StateMasterResponse toResponse(StateMaster entity) {
        return StateMasterResponse.builder()
                .id(entity.getId())
                .stateName(entity.getStateName())
                .stateCode(entity.getStateCode())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedAt(entity.getUpdatedAt())
                .updatedByUserId(entity.getUpdatedByUserId())
                .build();
    }
}
