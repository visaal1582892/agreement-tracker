package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.repository.RoleRightRepository;
import com.medplus.agreement_tracker_backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRightsService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRightRepository roleRightRepository;

    @Transactional(readOnly = true)
    public List<String> resolveRightsForUser(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getId())
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRightRepository.findRightCodesByRoleIds(roleIds).stream()
                .distinct()
                .sorted()
                .toList();
    }
}
