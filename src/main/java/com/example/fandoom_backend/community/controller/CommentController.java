package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.CommentLikeStatusResponse;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.dto.CreateCommentRequest;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import com.example.fandoom_backend.community.service.CommentInteractionService;
import com.example.fandoom_backend.community.service.CommentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentInteractionService commentInteractionService;

    @GetMapping("/threads/{slug}/comments")
    public PageResponse<CommentResponse> list(@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
                                               @PathVariable String slug,
                                               @RequestParam(defaultValue = "new") String sort,
                                               @PageableDefault(size = 20) Pageable pageable) {
        Long viewerId = principal == null ? null : principal.getId();
        return commentService.listForThread(slug, sort, viewerId, pageable);
    }

    @PostMapping("/threads/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable String slug,
                                   @Valid @RequestBody CommentRequest request) {
        return commentService.create(principal.getId(), slug, request);
    }

    // Merkezi/polimorfik uçlar (THREAD/BLOG/SEASON/EPISODE) — bkz.
    // CommentSubjectType. Eski /threads/{slug}/comments uçları FE uyumluluğu
    // için AYNEN kalır, buraya delege etmez (CommentServiceImpl'de tersi olur).
    @GetMapping("/comments")
    public PageResponse<CommentResponse> listForSubject(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal,
            @RequestParam CommentSubjectType subjectType,
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "new") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        Long viewerId = principal == null ? null : principal.getId();
        return commentService.listForSubject(subjectType, subjectId, sort, viewerId, pageable);
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createForSubject(@AuthenticationPrincipal CustomUserDetails principal,
                                             @Valid @RequestBody CreateCommentRequest request) {
        return commentService.createForSubject(principal.getId(), request.subjectType(), request.subjectId(),
                request.toCommentRequest());
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        commentService.delete(principal.getId(), isModerator(principal), id);
    }

    @PostMapping("/comments/{id}/like")
    public CommentLikeStatusResponse like(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long id) {
        return commentInteractionService.like(principal.getId(), id);
    }

    @DeleteMapping("/comments/{id}/like")
    public CommentLikeStatusResponse unlike(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable Long id) {
        return commentInteractionService.unlike(principal.getId(), id);
    }

    private boolean isModerator(CustomUserDetails principal) {
        Role role = principal.getRole();
        return role == Role.MODERATOR || role == Role.ADMIN;
    }
}
