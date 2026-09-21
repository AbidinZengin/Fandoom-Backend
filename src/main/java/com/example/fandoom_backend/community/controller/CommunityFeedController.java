package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.CommunityFeedService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community/feed")
@RequiredArgsConstructor
public class CommunityFeedController {

    private final CommunityFeedService communityFeedService;

    // scope: all (varsayılan) | joined. Geçersiz değer 400. joined için giriş şart: anonim istek SecurityConfig'te
    // (scope=joined matcher'ı) 401 alır; buradaki requireViewer yalnız savunma amaçlı.
    private static boolean isJoined(String scope) {
        if ("all".equalsIgnoreCase(scope)) {
            return false;
        }
        if ("joined".equalsIgnoreCase(scope)) {
            return true;
        }
        throw new InvalidReferenceException("Geçersiz scope (all|joined): " + scope);
    }

    private static Long requireViewer(Long viewerId) {
        if (viewerId == null) {
            throw new AccessDeniedException("scope=joined için giriş gerekli");
        }
        return viewerId;
    }

    // portal: portal slug'ı (yok/HIDDEN -> 404). productionSlug/tags: /threads ile aynı filtre semantiği (tags = OR).
    @GetMapping
    public PageResponse<ThreadSummaryResponse> getFeed(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) String productionSlug,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "all") String scope,
            @PageableDefault(size = 20) Pageable pageable) {
        Long viewerId = principal == null ? null : principal.getId();
        if (isJoined(scope)) {
            return communityFeedService.getJoinedFeed(
                    requireViewer(viewerId), surface, portal, productionSlug, tags, sort, pageable);
        }
        return communityFeedService.getFeed(surface, portal, productionSlug, tags, sort, viewerId, pageable);
    }

    // Ana sayfa akışı için OFFSET'siz (keyset) varyant: ilk istekte cursor verilmez,
    // sonraki isteklerde önceki yanıttaki nextCursor aynen geri gönderilir.
    @GetMapping("/cursor")
    public KeysetPageResponse<ThreadSummaryResponse> getFeedByCursor(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) String productionSlug,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String scope) {
        Long viewerId = principal == null ? null : principal.getId();
        if (isJoined(scope)) {
            return communityFeedService.getJoinedFeedByCursor(requireViewer(viewerId), surface, portal, productionSlug,
                    tags, sort, cursor, Math.clamp(size, 1, 50));
        }
        return communityFeedService.getFeedByCursor(
                surface, portal, productionSlug, tags, sort, viewerId, cursor, Math.clamp(size, 1, 50));
    }
}
