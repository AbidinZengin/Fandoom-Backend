package com.example.fandoom_backend.preference.controller;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.preference.dto.CommunityLayoutRequest;
import com.example.fandoom_backend.preference.dto.CommunityLayoutResponse;
import com.example.fandoom_backend.preference.service.CommunityLayoutService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/me/** SecurityConfig'te authenticated(); userId her zaman JWT'den çözülür. Hız sınırı: CommunityRateLimitFilter.
@RestController
@RequestMapping("/api/me/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private static final long MAX_BODY_BYTES = 1024;

    private final CommunityLayoutService communityLayoutService;

    @GetMapping("/community-layout")
    public ResponseEntity<CommunityLayoutResponse> getCommunityLayout(@AuthenticationPrincipal CustomUserDetails principal) {
        return communityLayoutService.get(principal.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/community-layout")
    public CommunityLayoutResponse putCommunityLayout(@AuthenticationPrincipal CustomUserDetails principal,
                                                      HttpServletRequest httpRequest,
                                                      @Valid @RequestBody CommunityLayoutRequest request) {
        if (httpRequest.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new InvalidReferenceException("İstek gövdesi 1 KB'ı aşamaz");
        }
        return communityLayoutService.put(principal.getId(), request);
    }
}
