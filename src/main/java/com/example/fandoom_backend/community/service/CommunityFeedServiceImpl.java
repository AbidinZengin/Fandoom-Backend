package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

// Faz 1: ThreadService.list'e ince bir delege. Faz 4'te kişiselleştirme
// (UserInterestProfile + FeedRankingService) burada devreye girecek — bilerek
// ayrı bir servis olarak tutuluyor ki /threads listeleme akışına dokunmadan
// genişletilebilsin. Portal (?portal=) filtresi de ThreadService.list'in portalSlug parametresine delege edilir.
@Service
@RequiredArgsConstructor
public class CommunityFeedServiceImpl implements CommunityFeedService {

    private final ThreadService threadService;

    @Override
    public PageResponse<ThreadSummaryResponse> getFeed(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, Pageable pageable) {
        return threadService.list(surface, portalSlug, productionSlug, tags, sort, viewerId, pageable);
    }

    @Override
    public PageResponse<ThreadSummaryResponse> getJoinedFeed(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, Pageable pageable) {
        return threadService.listJoined(viewerId, surface, portalSlug, productionSlug, tags, sort, pageable);
    }

    @Override
    public KeysetPageResponse<ThreadSummaryResponse> getJoinedFeedByCursor(
            Long viewerId, ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags,
            String sort, String cursor, int size) {
        return threadService.listJoinedByCursor(viewerId, surface, portalSlug, productionSlug, tags, sort, cursor, size);
    }

    @Override
    public KeysetPageResponse<ThreadSummaryResponse> getFeedByCursor(
            ThreadSurface surface, String portalSlug, String productionSlug, List<String> tags, String sort,
            Long viewerId, String cursor, int size) {
        return threadService.listByCursor(surface, portalSlug, productionSlug, tags, sort, viewerId, cursor, size);
    }
}
