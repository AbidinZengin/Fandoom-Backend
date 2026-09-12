package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.springframework.data.domain.Pageable;

public interface CommunityFeedService {

    PageResponse<ThreadSummaryResponse> getFeed(ThreadSurface surface, String sort, Pageable pageable);
}
