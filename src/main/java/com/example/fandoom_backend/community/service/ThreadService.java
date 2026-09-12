package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadPatchRequest;
import com.example.fandoom_backend.community.dto.ThreadRequest;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

public interface ThreadService {

    // viewerId: giriş yapmamış istekte null — isLiked/isBookmarked hep false
    // döner, ekstra sorgu atılmaz.
    PageResponse<ThreadSummaryResponse> list(
            ThreadSurface surface, String productionSlug, String tag, String sort, Long viewerId, Pageable pageable);

    ThreadDetailResponse getBySlug(String slug, Long viewerId);

    ThreadDetailResponse create(Long authorId, ThreadRequest request);

    ThreadDetailResponse update(Long userId, boolean moderator, String slug, ThreadPatchRequest request);

    void delete(Long userId, boolean moderator, String slug);
}
