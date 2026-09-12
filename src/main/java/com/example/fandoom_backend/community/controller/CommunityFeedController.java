package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/feed")
@RequiredArgsConstructor
public class CommunityFeedController {

    private final CommunityFeedService communityFeedService;

    @GetMapping
    public PageResponse<ThreadSummaryResponse> getFeed(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(defaultValue = "hot") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        Long viewerId = principal == null ? null : principal.getId();
        return communityFeedService.getFeed(surface, sort, viewerId, pageable);
    }
}
