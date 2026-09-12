package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.account.dto.BookmarkCountResponse;
import com.example.fandoom_backend.account.dto.FollowCountResponse;
import com.example.fandoom_backend.account.dto.LikeCountResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.UserBookmarkService;
import com.example.fandoom_backend.account.service.UserFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.account.service.UserListService;
import com.example.fandoom_backend.community.dto.UserProfileResponse;
import com.example.fandoom_backend.community.service.UserProfileService;
import com.example.fandoom_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
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

    private final UserListService userListService;
    private final UserLikeService userLikeService;
    private final UserFollowService userFollowService;
    private final UserBookmarkService userBookmarkService;
    private final UserProfileService userProfileService;
    private final UserService userService;

    @GetMapping("/api/users/{username}/lists/pinned")
    public List<UserListSummaryResponse> pinnedLists(@PathVariable String username) {
        Long userId = userService.getIdByUsername(username);
        return userListService.listPublicPinnedByUserId(userId);
    }

    @GetMapping("/api/users/{username}/profile")
    public UserProfileResponse profile(@PathVariable String username) {
        Long userId = userService.getIdByUsername(username);
        return userProfileService.getProfile(userId);
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
