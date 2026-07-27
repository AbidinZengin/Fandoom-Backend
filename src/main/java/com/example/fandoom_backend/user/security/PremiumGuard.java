package com.example.fandoom_backend.user.security;

import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("premiumGuard")
@RequiredArgsConstructor
public class PremiumGuard {

    private final UserService userService;

    public boolean isActive(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }
        return userService.isPremiumActive(principal.getId());
    }
}
