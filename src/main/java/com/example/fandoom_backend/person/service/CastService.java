package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.person.dto.CastRequest;
import com.example.fandoom_backend.person.dto.CastResponse;

import java.util.List;

public interface CastService {
    List<CastResponse> listForMovie(Long movieId);
    List<CastResponse> listForSeries(Long seriesId);
    CastResponse addToMovie(Long movieId, CastRequest request);
    CastResponse addToSeries(Long seriesId, CastRequest request);
    void delete(Long id);
}
