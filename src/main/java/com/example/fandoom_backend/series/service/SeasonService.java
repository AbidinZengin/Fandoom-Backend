package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonRequest;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;

import java.util.List;

public interface SeasonService {
    List<SeasonSummaryResponse> listBySeries(Long seriesId);
    SeasonDetailResponse getById(Long id);
    SeasonDetailResponse create(Long seriesId, SeasonRequest request);
    List<SeasonDetailResponse> createBatch(Long seriesId, List<SeasonRequest> requests);
    SeasonDetailResponse update(Long id, SeasonRequest request);
    void delete(Long id);
}
