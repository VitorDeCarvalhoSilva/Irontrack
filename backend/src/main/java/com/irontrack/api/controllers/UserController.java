package com.irontrack.api.controllers;

import com.irontrack.api.dto.request.CancelDeletionRequest;
import com.irontrack.api.dto.request.ChangePasswordRequest;
import com.irontrack.api.dto.request.DeleteAccountRequest;
import com.irontrack.api.dto.request.UpdateProfileRequest;
import com.irontrack.api.dto.response.DeletionScheduledResponse;
import com.irontrack.api.dto.response.UserResponse;
import com.irontrack.api.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 03_CONTRATOS_API.md §2.3, §2.8-§2.11 — controlador fino, delega toda regra
 * de negócio a {@link UserService} (01_ARQUITETURA_E_PADROES.md §2.2). O
 * {@code userId} do usuário autenticado é lido do {@link Authentication}
 * populado por {@code JwtAuthenticationFilter} (o {@code Authorization:
 * Bearer <token>} já foi validado antes de o controller ser alcançado).
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/v1/users/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.me(authentication.getName()));
    }

    @PatchMapping("/api/v1/users/me")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                       @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/api/v1/users/me/change-password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/users/me")
    public ResponseEntity<DeletionScheduledResponse> requestDeletion(Authentication authentication,
                                                                      @Valid @RequestBody DeleteAccountRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.requestDeletion(authentication.getName(), request));
    }

    // 03_CONTRATOS_API.md §2.11: path sob /auth, mas a regra de negócio vive em
    // UserService (07_ROADMAP_BACKEND.md §C.1, item 4) — endpoint público, sem
    // Authentication (a conta está bloqueada de login enquanto pendente de exclusão).
    @PostMapping("/api/v1/auth/cancel-deletion")
    public ResponseEntity<Void> cancelDeletion(@Valid @RequestBody CancelDeletionRequest request) {
        userService.cancelDeletion(request);
        return ResponseEntity.ok().build();
    }
}
