package com.example.fandoom_backend.genre.service;

import com.example.fandoom_backend.genre.dto.GenreRequest;
import com.example.fandoom_backend.genre.dto.GenreResponse;

import java.util.List;
import java.util.Set;

public interface GenreService {
    List<GenreResponse> list();
    GenreResponse getById(Long id);
    GenreResponse create(GenreRequest request);
    List<GenreResponse> createBatch(List<GenreRequest> requests);
    GenreResponse update(Long id, GenreRequest request);
    void delete(Long id);
    void assertAllExist(Set<Long> ids);
}
