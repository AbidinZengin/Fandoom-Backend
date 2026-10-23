package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.account.dto.BookmarkCountResponse;
import com.example.fandoom_backend.account.dto.FollowCountResponse;
import com.example.fandoom_backend.account.dto.FollowUserResponse;
import com.example.fandoom_backend.account.dto.LikeCountResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.FollowListService;
import com.example.fandoom_backend.account.service.UserBookmarkService;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.example.fandoom_backend.account.service.UserFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.community.dto.UserCommentResponse;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.service.ProfileContentService;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// account/'un public (auth gerektirmeyen) uçları — /api/me/** dışında kaldığı
// için ayrı bir controller (bkz. tasarım dokümanı modül ağacı).
// SecurityConfig'te path'ler ayrıca permitAll: /api/likes/*/*/count,
// /api/follows/*/*/count, /api/bookmarks/*/*/count, /api/users/*/lists/pinned,
// /api/users/*/profile.
@RestController
@RequiredArgsConstructor
public class PublicAccountController {

    private final UserLikeService userLikeService;
    private final UserFollowService userFollowService;
    private final UserBookmarkService userBookmarkService;
    private final FollowListService followListService;
    private final ProfileContentService profileContentService;
    private final UserProfileService userProfileService;
    private final UserService userService;

    // Gizli profilde kısıtlı izleyiciye boş liste (bkz. ProfileContentService).
    @GetMapping("/api/users/{username}/lists/pinned")
    public List<UserListSummaryResponse> pinnedLists(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String username) {
        return profileContentService.pinnedLists(username, principal == null ? null : principal.getId());
    }

    // Yalnız herkese açık CUSTOM listeler, pinliler önce.
    @GetMapping("/api/users/{username}/lists")
    public List<UserListSummaryResponse> publicLists(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String username) {
        return profileContentService.lists(username, principal == null ? null : principal.getId());
    }

    @GetMapping("/api/users/{username}/comments")
    public PageResponse<UserCommentResponse> comments(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String username, @PageableDefault(size = 20) Pageable pageable) {
        return profileContentService.comments(username, principal == null ? null : principal.getId(), pageable);
    }

    // Public uç — anonim istekte principal null gelebilir (bkz. community/ThreadController
    // aynı desen). isFollowing bu durumda hep false döner.
    @GetMapping("/api/users/{username}/profile")
    public UserProfileResponse profile(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
                                        @PathVariable String username) {
        Long userId = userService.getIdByUsername(username);
        Long viewerId = principal == null ? null : principal.getId();
        return userProfileService.getProfile(userId, viewerId);
    }

    // Public — anonim istekte principal null (following=false).
    @GetMapping("/api/users/{username}/followers")
    public PageResponse<FollowUserResponse> followers(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String username, @PageableDefault(size = 20) Pageable pageable) {
        return followListService.followers(username, principal == null ? null : principal.getId(), pageable);
    }

    @GetMapping("/api/users/{username}/following")
    public PageResponse<FollowUserResponse> following(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @PathVariable String username, @PageableDefault(size = 20) Pageable pageable) {
        return followListService.following(username, principal == null ? null : principal.getId(), pageable);
    }

    @GetMapping("/api/likes/{itemType}/{itemId}/count")
    public LikeCountResponse likeCount(@PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return new LikeCountResponse(userLikeService.count(itemType, itemId));
    }

    @GetMapping("/api/follows/{itemType}/{itemId}/count")
    public FollowCountResponse followCount(@PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return new FollowCountResponse(userFollowService.count(itemType, itemId));
    }

    @GetMapping("/api/bookmarks/{itemType}/{itemId}/count")
    public BookmarkCountResponse bookmarkCount(@PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return new BookmarkCountResponse(userBookmarkService.count(itemType, itemId));
    }
}
