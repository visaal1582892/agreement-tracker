package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.LoginRequest;
import com.medplus.agreement_tracker_backend.dto.response.AuthResponse;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getSession(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getSession(principal));
    }
}
