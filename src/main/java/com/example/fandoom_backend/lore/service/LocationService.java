package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.LocationRequest;
import com.example.fandoom_backend.lore.dto.LocationResponse;

import java.util.List;

public interface LocationService {
    List<LocationResponse> listForMovie(Long movieId);
    List<LocationResponse> listForSeries(Long seriesId);
    LocationResponse getById(Long id);
    LocationResponse getBySlug(String slug);
    LocationResponse addToMovie(Long movieId, LocationRequest request);
    LocationResponse addToSeries(Long seriesId, LocationRequest request);
    LocationResponse update(Long id, LocationRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
