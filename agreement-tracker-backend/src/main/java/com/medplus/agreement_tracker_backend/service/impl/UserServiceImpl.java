package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CreateUserRequest;
import com.medplus.agreement_tracker_backend.dto.response.UserResponse;
import com.medplus.agreement_tracker_backend.entity.Role;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.entity.UserRole;
import com.medplus.agreement_tracker_backend.enums.RoleName;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.RoleRepository;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.repository.UserRoleRepository;
import com.medplus.agreement_tracker_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, Long createdByUserId) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .employeeId(request.employeeId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isActive(true)
                .build();
        user.setCreatedByUserId(createdByUserId);
        user = userRepository.save(user);

        if (request.roles() != null) {
            assignRoles(user, request.roles(), createdByUserId);
        }

        return toResponse(user, request.roles() != null ? request.roles() : List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        List<String> roles = getRoleNames(id);
        return toResponse(user, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> toResponse(u, getRoleNames(u.getId())));
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long userId, List<String> roles, Long updatedByUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userRoleRepository.deleteAll(userRoleRepository.findByUserId(userId));
        assignRoles(user, roles, updatedByUserId);

        return toResponse(user, roles);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId, Long updatedByUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(false);
        user.setUpdatedByUserId(updatedByUserId);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String search) {
        return userRepository.searchActiveUsers(search)
                .stream()
                .map(u -> toResponse(u, getRoleNames(u.getId())))
                .toList();
    }

    private void assignRoles(User user, List<String> roleNames, Long createdByUserId) {
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(RoleName.valueOf(roleName))
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            UserRole userRole = UserRole.builder().user(user).role(role).build();
            userRole.setCreatedByUserId(createdByUserId);
            userRoleRepository.save(userRole);
        }
    }

    private List<String> getRoleNames(Long userId) {
        return userRoleRepository.findByUserIdWithRole(userId)
                .stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();
    }

    private UserResponse toResponse(User user, List<String> roles) {
        return new UserResponse(
                user.getId(), user.getEmployeeId(), user.getUsername(),
                user.getFullName(), user.getEmail(), user.isActive(),
                roles, user.getCreatedAt()
        );
    }
}
