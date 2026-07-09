package com.irontrack.api.controllers;

import com.irontrack.api.dto.request.ForgotPasswordRequest;
import com.irontrack.api.dto.request.LoginRequest;
import com.irontrack.api.dto.request.RefreshRequest;
import com.irontrack.api.dto.request.RegisterRequest;
import com.irontrack.api.dto.request.ResetPasswordRequest;
import com.irontrack.api.dto.response.AuthTokensResponse;
import com.irontrack.api.dto.response.MessageResponse;
import com.irontrack.api.dto.response.UserResponse;
import com.irontrack.api.dto.response.VerifyEmailResponse;
import com.irontrack.api.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 03_CONTRATOS_API.md §2.1-§2.7 — controlador fino, delega toda regra de
 * negócio a {@link AuthService} (01_ARQUITETURA_E_PADROES.md §2.2).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokensResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email/{token}")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@PathVariable String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
