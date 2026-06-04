package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.LoginRequest;
import com.medplus.agreement_tracker_backend.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
