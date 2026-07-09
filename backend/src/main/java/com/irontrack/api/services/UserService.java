package com.irontrack.api.services;

import com.irontrack.api.dto.request.CancelDeletionRequest;
import com.irontrack.api.dto.request.ChangePasswordRequest;
import com.irontrack.api.dto.request.DeleteAccountRequest;
import com.irontrack.api.dto.request.UpdateProfileRequest;
import com.irontrack.api.dto.response.DeletionScheduledResponse;
import com.irontrack.api.dto.response.UserResponse;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 4 — 03_CONTRATOS_API.md §2.3, §2.8-§2.11.
 */
public interface UserService {

    UserResponse me(String userId);

    UserResponse updateProfile(String userId, UpdateProfileRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    DeletionScheduledResponse requestDeletion(String userId, DeleteAccountRequest request);

    void cancelDeletion(CancelDeletionRequest request);
}
