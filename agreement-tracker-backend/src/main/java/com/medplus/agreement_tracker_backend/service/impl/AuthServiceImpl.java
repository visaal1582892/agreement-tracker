package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.LoginRequest;
import com.medplus.agreement_tracker_backend.dto.response.AuthResponse;
import com.medplus.agreement_tracker_backend.entity.UserRole;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.repository.UserRoleRepository;
import com.medplus.agreement_tracker_backend.security.JwtTokenProvider;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = jwtTokenProvider.generateToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        List<String> roles = userRoleRepository.findByUserIdWithRole(principal.getId())
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().name())
                .toList();

        var user = userRepository.findById(principal.getId()).orElseThrow();
        return new AuthResponse(token, principal.getId(), principal.getUsername(), user.getFullName(), principal.getEmail(), roles);
    }
}
