package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.community.service.PortalQueryService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Public portal okuma uçları (GET, SecurityConfig'te permitAll). Üyelik uçları: PortalMembershipController.
@RestController
@RequestMapping("/api/community/portals")
@RequiredArgsConstructor
public class PortalController {

    private final PortalQueryService portalQueryService;
    private final CommunityFeedService communityFeedService;

    @GetMapping
    public PageResponse<PortalSummaryResponse> list(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(defaultValue = "trending") String sort,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return portalQueryService.list(sort, q, viewerId(principal), pageable);
    }

    @GetMapping("/{slug}")
    public PortalDetailResponse getBySlug(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String slug) {
        return portalQueryService.getBySlug(slug, viewerId(principal));
    }

    // /api/community/feed?portal={slug} ile AYNI sonuç (aynı servis çağrısı).
    @GetMapping("/{slug}/feed")
    public PageResponse<ThreadSummaryResponse> feed(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String slug,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(defaultValue = "hot") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return communityFeedService.getFeed(surface, slug, null, null, sort, viewerId(principal), pageable);
    }

    private Long viewerId(CustomUserDetails principal) {
        return principal == null ? null : principal.getId();
    }
}
