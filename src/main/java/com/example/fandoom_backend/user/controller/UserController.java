package com.example.fandoom_backend.user.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.user.dto.UpdatePremiumRequest;
import com.example.fandoom_backend.user.dto.UpdateRoleRequest;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.dto.UserSummaryResponse;
import com.example.fandoom_backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userService.list(pageable);
    }

    @GetMapping("/{id}")
    public UserDetailResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PatchMapping("/{id}/role")
    public UserDetailResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(id, request.role());
    }

    @PatchMapping("/{id}/premium")
    public UserDetailResponse updatePremium(@PathVariable Long id, @RequestBody UpdatePremiumRequest request) {
        return userService.updatePremium(id, request.premiumExpiresAt());
    }
}
