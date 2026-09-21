package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Faz 1: CommunityFeedServiceImpl, ThreadService.list'e ince bir delege
// (productionSlug/tags her zaman null geçilir).
@ExtendWith(MockitoExtension.class)
class CommunityFeedServiceImplTest {

    @Mock private ThreadService threadService;

    private CommunityFeedServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommunityFeedServiceImpl(threadService);
    }

    @Test
    void getFeed_delegatesToThreadServiceListWithNullProductionSlugAndTags() {
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<ThreadSummaryResponse> expected = PageResponse.from(Page.empty(pageable));
        when(threadService.list(ThreadSurface.THEORY, null, null, null, "hot", 9L, pageable)).thenReturn(expected);

        PageResponse<ThreadSummaryResponse> result = service.getFeed(ThreadSurface.THEORY, null, null, null, "hot", 9L, pageable);

        assertThat(result).isSameAs(expected);
        verify(threadService).list(ThreadSurface.THEORY, null, null, null, "hot", 9L, pageable);
    }

    @Test
    void getFeed_surfaceNull_stillDelegatesWithNullSurface() {
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<ThreadSummaryResponse> expected = PageResponse.from(Page.empty(pageable));
        when(threadService.list(null, null, null, null, "new", null, pageable)).thenReturn(expected);

        PageResponse<ThreadSummaryResponse> result = service.getFeed(null, null, null, null, "new", null, pageable);

        assertThat(result).isSameAs(expected);
        verify(threadService).list(null, null, null, null, "new", null, pageable);
    }
}
