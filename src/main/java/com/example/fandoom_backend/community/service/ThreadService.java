package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ThreadService {

    // viewerId: giriş yapmamış istekte null — isLiked/isBookmarked hep false
    // döner, ekstra sorgu atılmaz. tags: null/boş ise filtre uygulanmaz, birden
    // fazla tag VEYA (OR) mantığıyla birleşir (herhangi birine sahip thread'ler).
    PageResponse<ThreadSummaryResponse> list(
            ThreadSurface surface, String productionSlug, List<String> tags, String sort, Long viewerId,
            Pageable pageable);

    // Keyset (cursor) varyantı — derin sayfada OFFSET/COUNT maliyeti yok. cursor: önceki
    // yanıttaki nextCursor (ilk sayfada null).
    KeysetPageResponse<ThreadSummaryResponse> listByCursor(
            ThreadSurface surface, String productionSlug, List<String> tags, String sort, Long viewerId,
            String cursor, int size);

    ThreadDetailResponse getBySlug(String slug, Long viewerId);

    ThreadDetailResponse create(Long authorId, ThreadRequest request);

    ThreadDetailResponse update(Long userId, boolean moderator, String slug, ThreadPatchRequest request);

    void delete(Long userId, boolean moderator, String slug);
}
