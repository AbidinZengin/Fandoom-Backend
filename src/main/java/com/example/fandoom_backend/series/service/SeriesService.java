package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesRequest;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface SeriesService {
    PageResponse<SeriesSummaryResponse> list(Pageable pageable);
    PageResponse<SeriesSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable);
    PageResponse<SeriesSummaryResponse> search(String query, Pageable pageable);
    SeriesDetailResponse getById(Long id);
    SeriesDetailResponse getBySlug(String slug);
    SeriesDetailResponse create(SeriesRequest request);
    SeriesDetailResponse update(Long id, SeriesRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
