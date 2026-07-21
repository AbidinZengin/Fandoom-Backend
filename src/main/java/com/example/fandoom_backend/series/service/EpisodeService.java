package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.series.dto.EpisodeRequest;
import com.example.fandoom_backend.series.dto.EpisodeResponse;

import java.util.List;

public interface EpisodeService {
    List<EpisodeResponse> listBySeason(Long seasonId);
    EpisodeResponse getById(Long id);
    EpisodeResponse create(Long seasonId, EpisodeRequest request);
    List<EpisodeResponse> createBatch(Long seasonId, List<EpisodeRequest> requests);
    EpisodeResponse update(Long id, EpisodeRequest request);
    void delete(Long id);
}
