package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommunityFeedService {

    // portalSlug/productionSlug/tags: null = filtre yok (bkz. ThreadService.list).
    PageResponse<ThreadSummaryResponse> getFeed(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, Pageable pageable);

    // scope=joined: yalnız viewerId'nin üye olduğu portallar (portal/surface/sort/productionSlug/tags ile birleşir).
    // Kullanıcıya özel -> cache'lenmez.
    PageResponse<ThreadSummaryResponse> getJoinedFeed(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, Pageable pageable);

    KeysetPageResponse<ThreadSummaryResponse> getJoinedFeedByCursor(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, String cursor, int size);

    KeysetPageResponse<ThreadSummaryResponse> getFeedByCursor(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, String cursor, int size);
}
