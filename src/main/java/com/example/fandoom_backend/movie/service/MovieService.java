package com.example.fandoom_backend.movie.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieRequest;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface MovieService {
    PageResponse<MovieSummaryResponse> list(Pageable pageable);
    PageResponse<MovieSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable);
    PageResponse<MovieSummaryResponse> search(String query, Pageable pageable);
    MovieDetailResponse getById(Long id);
    MovieDetailResponse getBySlug(String slug);
    MovieDetailResponse create(MovieRequest request);
    MovieDetailResponse update(Long id, MovieRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
