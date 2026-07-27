package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.person.dto.CastRequest;
import com.example.fandoom_backend.person.dto.CastResponse;

import java.util.List;

public interface CastService {
    List<CastResponse> listForMovie(Long movieId);
    List<CastResponse> listForSeries(Long seriesId);
    CastResponse addToMovie(Long movieId, CastRequest request);
    List<CastResponse> addToMovieBatch(Long movieId, List<CastRequest> requests);
    CastResponse addToSeries(Long seriesId, CastRequest request);
    List<CastResponse> addToSeriesBatch(Long seriesId, List<CastRequest> requests);
    void delete(Long id);
}
