package com.example.fandoom_backend.movie.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieRequest;
import com.example.fandoom_backend.movie.dto.MovieSearchResult;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MovieService {
    PageResponse<MovieSummaryResponse> list(Pageable pageable);
    PageResponse<MovieSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable);
    PageResponse<MovieSummaryResponse> listByGenre(Long genreId, Pageable pageable);
    // search/ modülünün full search kapısı: quickSearch ile aynı eşleşme/sıra, sayfalı. pageable unsorted olmalı.
    PageResponse<MovieSummaryResponse> fullSearch(String query, Pageable pageable);
    // search/ modülünün quick search kapısı. query'nin trim/uzunluk kuralları çağıranın işidir;
    // burada yalnızca LIKE jokerleri kaçışlanır. Sıra: "ile başlayan" önce, sonra alfabetik (title).
    List<MovieSearchResult> quickSearch(String query, int limit);
    MovieDetailResponse getById(Long id);
    MovieDetailResponse getBySlug(String slug);
    MovieDetailResponse create(MovieRequest request);
    List<MovieDetailResponse> createBatch(List<MovieRequest> requests);
    MovieDetailResponse update(Long id, MovieRequest request);
    void delete(Long id);
    boolean existsById(Long id);
    // community/ThreadServiceImpl productionSlug doğrulaması için — cross-module
    // referans doğrulaması servis interface'i üzerinden yapılır.
    boolean existsBySlug(String slug);
}
