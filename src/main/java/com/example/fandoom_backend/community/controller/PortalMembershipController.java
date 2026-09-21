package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.community.dto.PortalMembershipStatusResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.service.PortalMembershipService;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Üyelik uçları (hepsi giriş ister — SecurityConfig: POST/DELETE .../portals/*/join ve /api/me/**).
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PortalMembershipController {

    private final PortalMembershipService portalMembershipService;
    private final PortalQueryService portalQueryService;

    @PostMapping("/community/portals/{slug}/join")
    public PortalMembershipStatusResponse join(@AuthenticationPrincipal CustomUserDetails principal,
                                               @PathVariable String slug) {
        return portalMembershipService.join(principal.getId(), slug);
    }

    @DeleteMapping("/community/portals/{slug}/join")
    public PortalMembershipStatusResponse leave(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable String slug) {
        return portalMembershipService.leave(principal.getId(), slug);
    }

    // Portallarım: düz List (sayfalama yok). Varsayılan sıra joinedAt azalan; ?sort=activity -> son 7 gün aktivitesine göre.
    @GetMapping("/me/portals")
    public List<PortalSummaryResponse> myPortals(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @RequestParam(required = false) String sort) {
        return portalQueryService.listJoined(principal.getId(), sort);
    }
}
