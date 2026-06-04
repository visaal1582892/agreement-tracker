package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.RoleRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.RoleResponse;
import com.medplus.agreement_tracker_backend.entity.Role;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<RoleResponse> search(MasterPageRequest req) {
        Specification<Role> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<Role> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<Role> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    public RoleResponse create(RoleRequest req, Long userId) {
        if (repository.findByName(req.getName()).isPresent()) {
            throw new DuplicateResourceException("Role already exists: " + req.getName());
        }
        Role entity = Role.builder().name(req.getName()).description(req.getDescription()).build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public RoleResponse update(Long id, RoleRequest req, Long userId) {
        Role entity = findOrThrow(id);
        if (!entity.getName().equals(req.getName()) && repository.findByName(req.getName()).isPresent()) {
            throw new DuplicateResourceException("Role already exists: " + req.getName());
        }
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        Role entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private Role findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    private Specification<Role> buildSpec(Map<String, String> filters) {
        Specification<Role> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("description")) spec = spec.and(SpecificationUtils.stringLike("description", filters.get("description")));
        if (filters.containsKey("isActive")) spec = spec.and(SpecificationUtils.booleanEquals("isActive", SpecificationUtils.parseBoolean(filters.get("isActive"))));
        return spec;
    }

    private RoleResponse toResponse(Role e) {
        return RoleResponse.builder()
                .id(e.getId()).name(e.getName()).description(e.getDescription()).isActive(e.isActive())
                .createdAt(e.getCreatedAt()).createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt()).updatedByUserId(e.getUpdatedByUserId()).build();
    }
}
