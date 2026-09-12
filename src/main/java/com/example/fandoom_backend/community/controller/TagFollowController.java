package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.FollowedTagResponse;
import com.example.fandoom_backend.community.dto.TagFollowStatusResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.dto.TrendingTagResponse;
import com.example.fandoom_backend.community.service.TagFollowService;
import com.example.fandoom_backend.community.service.ThreadService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// /{tag}/threads, ThreadService.list'e ince bir delege — mevcut ?tag= filtresiyle
// aynı mantık, sadece daha temiz bir URL (bkz. ThreadServiceImpl.list).
@RestController
@RequestMapping("/api/community/tags")
@RequiredArgsConstructor
public class TagFollowController {

    private final TagFollowService tagFollowService;
    private final ThreadService threadService;

    @PostMapping("/{tag}/follow")
    public TagFollowStatusResponse follow(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable String tag) {
        return tagFollowService.follow(principal.getId(), tag);
    }

    @DeleteMapping("/{tag}/follow")
    public TagFollowStatusResponse unfollow(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable String tag) {
        return tagFollowService.unfollow(principal.getId(), tag);
    }

    // Kendi hesabını gösterir, SecurityConfig'te özel bir kural yok —
    // anyRequest().authenticated()'a düşer, bu doğru davranış.
    @GetMapping("/followed")
    public List<FollowedTagResponse> listFollowed(@AuthenticationPrincipal CustomUserDetails principal) {
        return tagFollowService.listFollowed(principal.getId());
    }

    @GetMapping("/trending")
    public List<TrendingTagResponse> trending(
            @RequestParam(defaultValue = "7d") String window,
            @RequestParam(defaultValue = "10") int limit) {
        return tagFollowService.getTrending(window, limit);
    }

    @GetMapping("/{tag}/threads")
    public PageResponse<ThreadSummaryResponse> threadsByTag(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String tag,
            @RequestParam(defaultValue = "hot") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        Long viewerId = principal == null ? null : principal.getId();
        return threadService.list(null, null, tag, sort, viewerId, pageable);
    }
}
