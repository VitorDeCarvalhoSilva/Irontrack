package com.irontrack.api.services;

import com.irontrack.api.dto.request.ForgotPasswordRequest;
import com.irontrack.api.dto.request.LoginRequest;
import com.irontrack.api.dto.request.RefreshRequest;
import com.irontrack.api.dto.request.RegisterRequest;
import com.irontrack.api.dto.request.ResetPasswordRequest;
import com.irontrack.api.dto.response.AuthTokensResponse;
import com.irontrack.api.dto.response.MessageResponse;
import com.irontrack.api.dto.response.UserResponse;
import com.irontrack.api.dto.response.VerifyEmailResponse;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 3 — 03_CONTRATOS_API.md §2.1-§2.7.
 */
public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthTokensResponse login(LoginRequest request);

    AuthTokensResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

    VerifyEmailResponse verifyEmail(String rawToken);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
