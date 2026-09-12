package com.example.fandoom_backend.account.controller;

import com.example.fandoom_backend.account.dto.CreateUserListRequest;
import com.example.fandoom_backend.account.dto.FollowStatusResponse;
import com.example.fandoom_backend.account.dto.LikeStatusResponse;
import com.example.fandoom_backend.account.dto.SaveItemRequest;
import com.example.fandoom_backend.account.dto.SavedItemStatusResponse;
import com.example.fandoom_backend.account.dto.UpdateSavedItemRequest;
import com.example.fandoom_backend.account.dto.UpdateUserProfileRequest;
import com.example.fandoom_backend.account.dto.UserFollowResponse;
import com.example.fandoom_backend.account.dto.UserLikeResponse;
import com.example.fandoom_backend.account.dto.UserListDetailResponse;
import com.example.fandoom_backend.account.dto.UserListSummaryResponse;
import com.example.fandoom_backend.account.dto.UserProfileResponse;
import com.example.fandoom_backend.account.dto.UserSavedItemResponse;
import com.example.fandoom_backend.account.dto.BookmarkStatusResponse;
import com.example.fandoom_backend.account.dto.UserBookmarkResponse;
import com.example.fandoom_backend.account.entity.ListType;
import com.example.fandoom_backend.account.entity.SavedItemType;
import com.example.fandoom_backend.account.service.UserBookmarkService;
import com.example.fandoom_backend.account.service.UserFollowService;
import com.example.fandoom_backend.account.service.UserLikeService;
import com.example.fandoom_backend.account.service.UserListService;
import com.example.fandoom_backend.account.service.UserProfileService;
import com.example.fandoom_backend.account.service.UserSavedItemService;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.validation.OnCreate;
import com.example.fandoom_backend.common.validation.OnUpdate;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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

import java.util.List;

// Bu controller altındaki HER metod, userId'yi @AuthenticationPrincipal
// üzerinden JWT'den çözer — hiçbir request DTO'sunda userId YOK (IDOR
// önleme, bkz. CLAUDE.md "Kimlik Doğrulama"). SecurityConfig'te /api/me/**
// için ayrı bir kural yok, otomatik anyRequest().authenticated()'a düşüyor.
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountController {

    private final UserProfileService userProfileService;
    private final UserListService userListService;
    private final UserSavedItemService userSavedItemService;
    private final UserLikeService userLikeService;
    private final UserFollowService userFollowService;
    private final UserBookmarkService userBookmarkService;

    // ---- profile ----

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return userProfileService.getProfile(principal.getId());
    }

    // @Valid burada güvenle kullanılabilir: DTO'daki tüm kısıtlar (@Size/@Pattern)
    // null değeri otomatik geçerli sayar, null=değişmedi kısmi güncelleme
    // semantiği bozulmaz (bkz. UpdateUserProfileRequest).
    @PatchMapping("/profile")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                              @Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateProfile(principal.getId(), request);
    }

    // ---- lists ----

    @GetMapping("/lists")
    public List<UserListSummaryResponse> listLists(@AuthenticationPrincipal CustomUserDetails principal) {
        return userListService.list(principal.getId());
    }

    @GetMapping("/lists/{id}")
    public UserListDetailResponse getList(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long id,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return userListService.getById(principal.getId(), id, pageable);
    }

    @PostMapping("/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public UserListDetailResponse createList(@AuthenticationPrincipal CustomUserDetails principal,
                                              @Validated(OnCreate.class) @RequestBody CreateUserListRequest request) {
        return userListService.create(principal.getId(), request);
    }

    // @Validated(OnUpdate.class): CreateUserListRequest.title OnCreate grubunda
    // @NotBlank taşır (POST'ta zorunlu) — PATCH'te null=değişmedi olduğu için
    // OnUpdate grubu kullanılır (bkz. CreateUserListRequest).
    @PatchMapping("/lists/{id}")
    public UserListDetailResponse updateList(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable Long id,
                                              @Validated(OnUpdate.class) @RequestBody CreateUserListRequest request) {
        return userListService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/lists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteList(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        userListService.delete(principal.getId(), id);
    }

    @PatchMapping("/lists/{id}/pin")
    public UserListSummaryResponse togglePin(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable Long id) {
        return userListService.togglePin(principal.getId(), id);
    }

    // ---- saved items ----

    @PostMapping("/saved-items")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSavedItemResponse saveItem(@AuthenticationPrincipal CustomUserDetails principal,
                                           @Valid @RequestBody SaveItemRequest request) {
        return userSavedItemService.save(principal.getId(), request);
    }

    @PatchMapping("/saved-items/{id}")
    public UserSavedItemResponse updateSavedItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody UpdateSavedItemRequest request) {
        return userSavedItemService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/saved-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavedItem(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        userSavedItemService.delete(principal.getId(), id);
    }

    // listType opsiyonel — verilmezse itemType'ın varsayılan sistem listesine
    // (BLOG->READLIST, MOVIE|SERIES->WATCHLIST) göre kontrol eder; WATCHED
    // durumunu sormak için ?listType=WATCHED geçilir.
    @GetMapping("/saved-items/status/{itemType}/{itemId}")
    public SavedItemStatusResponse savedItemStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @PathVariable SavedItemType itemType, @PathVariable Long itemId,
                                                     @RequestParam(required = false) ListType listType) {
        return userSavedItemService.getStatus(principal.getId(), itemType, itemId, listType);
    }

    // ---- bookmarks (Save — liste kavramından tamamen bağımsız, Like/Follow
    // ile aynı desen; Watchlist/Watched'e (yukarıdaki saved-items) hiç dokunmaz) ----

    @PostMapping("/bookmarks/{itemType}/{itemId}")
    public BookmarkStatusResponse bookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userBookmarkService.bookmark(principal.getId(), itemType, itemId);
    }

    @DeleteMapping("/bookmarks/{itemType}/{itemId}")
    public BookmarkStatusResponse unbookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userBookmarkService.unbookmark(principal.getId(), itemType, itemId);
    }

    @GetMapping("/bookmarks/{itemType}/{itemId}")
    public BookmarkStatusResponse bookmarkStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userBookmarkService.getStatus(principal.getId(), itemType, itemId);
    }

    @GetMapping("/bookmarks")
    public PageResponse<UserBookmarkResponse> listBookmarks(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return userBookmarkService.list(principal.getId(), pageable);
    }

    // ---- likes ----

    @PostMapping("/likes/{itemType}/{itemId}")
    public LikeStatusResponse like(@AuthenticationPrincipal CustomUserDetails principal,
                                    @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userLikeService.like(principal.getId(), itemType, itemId);
    }

    @DeleteMapping("/likes/{itemType}/{itemId}")
    public LikeStatusResponse unlike(@AuthenticationPrincipal CustomUserDetails principal,
                                      @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userLikeService.unlike(principal.getId(), itemType, itemId);
    }

    @GetMapping("/likes/{itemType}/{itemId}")
    public LikeStatusResponse likeStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userLikeService.getStatus(principal.getId(), itemType, itemId);
    }

    @GetMapping("/likes")
    public PageResponse<UserLikeResponse> listLikes(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return userLikeService.list(principal.getId(), pageable);
    }

    // ---- follows ----

    @PostMapping("/follows/{itemType}/{itemId}")
    public FollowStatusResponse follow(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userFollowService.follow(principal.getId(), itemType, itemId);
    }

    @DeleteMapping("/follows/{itemType}/{itemId}")
    public FollowStatusResponse unfollow(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userFollowService.unfollow(principal.getId(), itemType, itemId);
    }

    @GetMapping("/follows/{itemType}/{itemId}")
    public FollowStatusResponse followStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable SavedItemType itemType, @PathVariable Long itemId) {
        return userFollowService.getStatus(principal.getId(), itemType, itemId);
    }

    @GetMapping("/follows")
    public PageResponse<UserFollowResponse> listFollows(@AuthenticationPrincipal CustomUserDetails principal,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return userFollowService.list(principal.getId(), pageable);
    }
}
