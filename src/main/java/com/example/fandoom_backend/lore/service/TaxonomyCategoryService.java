package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.TaxonomyCategoryRequest;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;

import java.util.List;

public interface TaxonomyCategoryService {
    List<TaxonomyCategoryResponse> listForMovie(Long movieId);
    List<TaxonomyCategoryResponse> listForSeries(Long seriesId);
    TaxonomyCategoryResponse getById(Long id);
    TaxonomyCategoryResponse getBySlug(String slug);
    TaxonomyCategoryResponse create(TaxonomyCategoryRequest request);
    TaxonomyCategoryResponse addToMovie(Long movieId, TaxonomyCategoryRequest request);
    TaxonomyCategoryResponse addToSeries(Long seriesId, TaxonomyCategoryRequest request);
    TaxonomyCategoryResponse update(Long id, TaxonomyCategoryRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
