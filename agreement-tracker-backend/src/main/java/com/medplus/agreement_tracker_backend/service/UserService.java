package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CreateUserRequest;
import com.medplus.agreement_tracker_backend.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request, Long createdByUserId);
    UserResponse getUserById(Long id);
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUserRoles(Long userId, List<String> roles, Long updatedByUserId);
    void deactivateUser(Long userId, Long updatedByUserId);
    List<UserResponse> searchUsers(String search);
}
