package com.example.fandoom_backend.account.service;

import com.example.fandoom_backend.account.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.account.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);
}
