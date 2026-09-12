package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// Faz 1: ThreadService.list'e ince bir delege. Faz 4'te kişiselleştirme
// (UserInterestProfile + FeedRankingService) burada devreye girecek — bilerek
// ayrı bir servis olarak tutuluyor ki /threads listeleme akışına dokunmadan
// genişletilebilsin.
@Service
@RequiredArgsConstructor
public class CommunityFeedServiceImpl implements CommunityFeedService {

    private final ThreadService threadService;

    @Override
    public PageResponse<ThreadSummaryResponse> getFeed(ThreadSurface surface, String sort, Pageable pageable) {
        return threadService.list(surface, null, null, sort, pageable);
    }
}
