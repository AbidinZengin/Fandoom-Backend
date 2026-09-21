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

import java.util.List;

// {id:\d+} birincil erişim yoludur; slug uçları (@Deprecated) yalnızca SAYISAL OLMAYAN slug'ları yakalar
// ({slug:(?!\d+$).+}): SlugGenerator başlıktan saf-sayısal slug üretebilir (başlık "1234567890" -> slug "1234567890"),
// bu durumda /threads/1234567890 belirsiz kalırdı — sayısal segment HER ZAMAN id olarak yorumlanır.
@RestController
@RequestMapping("/api/community/threads")
@RequiredArgsConstructor
public class ThreadController {

    private static final String SLUG = "{slug:(?!\\d+$).+}";
    private static final String ID = "{id:\\d+}";

    private final ThreadService threadService;
    private final ThreadInteractionService threadInteractionService;

    @GetMapping
    public PageResponse<ThreadSummaryResponse> list(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam(required = false) ThreadSurface surface,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) String productionSlug,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "hot") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return threadService.list(surface, portal, productionSlug, tags, sort, viewerId(principal), pageable);
    }

    @GetMapping("/" + ID)
    public ThreadDetailResponse getById(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
                                         @PathVariable Long id) {
        return threadService.getById(id, viewerId(principal));
    }

    /**
     * @deprecated slug başlık PATCH'lenince değişir; {@code GET /api/community/threads/{id}} kullanın.
     */
    @Deprecated
    @GetMapping("/" + SLUG)
    public ThreadDetailResponse getBySlug(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
                                           @PathVariable String slug) {
        return threadService.getBySlug(slug, viewerId(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadDetailResponse create(@AuthenticationPrincipal CustomUserDetails principal,
                                        @Valid @RequestBody ThreadRequest request) {
        return threadService.create(principal.getId(), isModerator(principal), request);
    }

    // PATCH: portalSlug ile portal DEĞİŞTİRME yalnız MODERATOR/ADMIN'e açıktır (bkz. ThreadServiceImpl.moveIfRequested).
    @PatchMapping("/" + ID)
    public ThreadDetailResponse updateById(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ThreadPatchRequest request) {
        return threadService.update(principal.getId(), isModerator(principal), id, request);
    }

    @DeleteMapping("/" + ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        threadService.delete(principal.getId(), isModerator(principal), id);
    }

    /** @deprecated slug başlıkla değişir; {@code PATCH /api/community/threads/{id}} kullanın. */
    @Deprecated
    @PatchMapping("/" + SLUG)
    public ThreadDetailResponse update(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable String slug,
                                        @Valid @RequestBody ThreadPatchRequest request) {
        return threadService.update(principal.getId(), isModerator(principal), slug, request);
    }

    /** @deprecated {@code DELETE /api/community/threads/{id}} kullanın. */
    @Deprecated
    @DeleteMapping("/" + SLUG)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String slug) {
        threadService.delete(principal.getId(), isModerator(principal), slug);
    }

    @PostMapping("/" + ID + "/like")
    public ThreadLikeStatusResponse like(@AuthenticationPrincipal CustomUserDetails principal,
                                          @PathVariable Long id) {
        return threadInteractionService.like(principal.getId(), id);
    }

    @DeleteMapping("/" + ID + "/like")
    public ThreadLikeStatusResponse unlike(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable Long id) {
        return threadInteractionService.unlike(principal.getId(), id);
    }

    @PostMapping("/" + ID + "/bookmark")
    public ThreadBookmarkStatusResponse bookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long id) {
        return threadInteractionService.bookmark(principal.getId(), id);
    }

    @DeleteMapping("/" + ID + "/bookmark")
    public ThreadBookmarkStatusResponse unbookmark(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @PathVariable Long id) {
        return threadInteractionService.unbookmark(principal.getId(), id);
    }

    /** @deprecated {@code POST /api/community/threads/{id}/like} kullanın. */
    @Deprecated
    @PostMapping("/" + SLUG + "/like")
    public ThreadLikeStatusResponse likeBySlug(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable String slug) {
        return threadInteractionService.like(principal.getId(), slug);
    }

    /** @deprecated {@code DELETE /api/community/threads/{id}/like} kullanın. */
    @Deprecated
    @DeleteMapping("/" + SLUG + "/like")
    public ThreadLikeStatusResponse unlikeBySlug(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable String slug) {
        return threadInteractionService.unlike(principal.getId(), slug);
    }

    /** @deprecated {@code POST /api/community/threads/{id}/bookmark} kullanın. */
    @Deprecated
    @PostMapping("/" + SLUG + "/bookmark")
    public ThreadBookmarkStatusResponse bookmarkBySlug(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @PathVariable String slug) {
        return threadInteractionService.bookmark(principal.getId(), slug);
    }

    /** @deprecated {@code DELETE /api/community/threads/{id}/bookmark} kullanın. */
    @Deprecated
    @DeleteMapping("/" + SLUG + "/bookmark")
    public ThreadBookmarkStatusResponse unbookmarkBySlug(@AuthenticationPrincipal CustomUserDetails principal,
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
