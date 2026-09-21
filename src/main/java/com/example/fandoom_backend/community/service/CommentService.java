package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.CommentRequest;
import com.example.fandoom_backend.community.dto.CommentResponse;
import com.example.fandoom_backend.community.entity.CommentSubjectType;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    // viewerId: giriş yapmamış istekte null — isLiked hep false döner, ekstra
    // sorgu atılmaz. THREAD'e özel eski uç — slug'ı içeride subjectId'ye
    // çözüp listForSubject'e delege eder (geriye uyumluluk için korunuyor).
    // Id varyantları birincil yol (slug başlık PATCH'lenince değişir). Yok/DELETED/HIDDEN thread ve portalı HIDDEN
    // olan thread -> 404.
    PageResponse<CommentResponse> listForThread(Long threadId, String sort, Long viewerId, Pageable pageable);

    CommentResponse create(Long authorId, Long threadId, CommentRequest request);

    KeysetPageResponse<CommentResponse> listForThread(
            Long threadId, String sort, Long viewerId, String cursor, int size);

    /** @deprecated slug değişebilir; {@link #listForThread(Long, String, Long, Pageable)} kullanın. */
    @Deprecated
    PageResponse<CommentResponse> listForThread(String threadSlug, String sort, Long viewerId, Pageable pageable);

    /** @deprecated slug değişebilir; {@link #create(Long, Long, CommentRequest)} kullanın. */
    @Deprecated
    CommentResponse create(Long authorId, String threadSlug, CommentRequest request);

    // Merkezi/polimorfik uçlar (THREAD/BLOG/SEASON/EPISODE) — bkz.
    // /api/community/comments. THREAD için subjectId = Thread.id.
    PageResponse<CommentResponse> listForSubject(
            CommentSubjectType subjectType, Long subjectId, String sort, Long viewerId, Pageable pageable);

    // Keyset (cursor) varyantları — COUNT/OFFSET yok; cursor = önceki yanıttaki nextCursor.
    /** @deprecated slug değişebilir; {@link #listForThread(Long, String, Long, String, int)} kullanın. */
    @Deprecated
    KeysetPageResponse<CommentResponse> listForThreadByCursor(
            String threadSlug, String sort, Long viewerId, String cursor, int size);

    KeysetPageResponse<CommentResponse> listForSubjectByCursor(
            CommentSubjectType subjectType, Long subjectId, String sort, Long viewerId, String cursor, int size);

    CommentResponse createForSubject(
            Long authorId, CommentSubjectType subjectType, Long subjectId, CommentRequest request);

    void delete(Long userId, boolean moderator, Long commentId);
}
