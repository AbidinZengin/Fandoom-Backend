package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadBookmarkStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadLikeStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.community.service.ThreadInteractionService;
import com.example.fandoom_backend.community.service.ThreadService;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;
    private final ThreadInteractionService threadInteractionService;

    @GetMapping
    public PageResponse<ThreadSummaryResponse> list(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(required = false) String productionSlug,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "hot") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return threadService.list(surface, productionSlug, tag, sort, viewerId(principal), pageable);
    }

    @GetMapping("/{slug}")
    public ThreadDetailResponse getBySlug(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
                                           @PathVariable String slug) {
        return threadService.getBySlug(slug, viewerId(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse create(@AuthenticationPrincipal CustomUserDetails principal,
                                        @Valid @RequestBody ThreadRequest request) {
        return threadService.create(principal.getId(), request);
    }

    @PatchMapping("/{slug}")
    public ThreadDetailResponse update(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable String slug,
                                        @Valid @RequestBody ThreadPatchRequest request) {
        return threadService.update(principal.getId(), isModerator(principal), slug, request);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String slug) {
        threadService.delete(principal.getId(), isModerator(principal), slug);
    }

    @PostMapping("/{slug}/like")
    public ThreadLikeStatusResponse like(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable String slug) {
        return threadInteractionService.like(principal.getId(), slug);
    }

    @DeleteMapping("/{slug}/like")
    public ThreadLikeStatusResponse unlike(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable String slug) {
        return threadInteractionService.unlike(principal.getId(), slug);
    }

    @PostMapping("/{slug}/bookmark")
    public ThreadBookmarkStatusResponse bookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable String slug) {
        return threadInteractionService.bookmark(principal.getId(), slug);
    }

    @DeleteMapping("/{slug}/bookmark")
    public ThreadBookmarkStatusResponse unbookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @PathVariable String slug) {
        return threadInteractionService.unbookmark(principal.getId(), slug);
    }

    // Owner/moderator ayrımı path seviyesinde ifade edilemediği için (aynı path
    // hem sahibinin hem moderatörün isteğini kabul eder) burada, serviste kontrol
    // edilir (bkz. SecurityConfig'teki not).
    private boolean isModerator(CustomUserDetails principal) {
        Role role = principal.getRole();
        return role == Role.MODERATOR || role == Role.ADMIN;
    }

    // GET uçları anonim isteklere de açık olabileceğinden (bkz. SecurityConfig)
    // principal null gelebilir — isLiked/isBookmarked bu durumda hep false döner.
    private Long viewerId(CustomUserDetails principal) {
        return principal == null ? null : principal.getId();
    }
}
