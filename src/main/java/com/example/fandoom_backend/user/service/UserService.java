package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.user.dto.RegisterRequest;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.dto.UserSummaryResponse;
import com.example.fandoom_backend.user.entity.Role;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface UserService {
    UserDetailResponse register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    PageResponse<UserSummaryResponse> list(Pageable pageable);
    UserDetailResponse getById(Long id);
    UserDetailResponse updateRole(Long id, Role role);
    UserDetailResponse updatePremium(Long id, LocalDateTime premiumExpiresAt);
    boolean isPremiumActive(Long id);
    boolean existsById(Long id);
}
