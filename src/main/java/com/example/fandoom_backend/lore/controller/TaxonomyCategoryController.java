package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.TaxonomyCategoryRequest;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;
import com.example.fandoom_backend.lore.service.TaxonomyCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaxonomyCategoryController {

    private final TaxonomyCategoryService taxonomyCategoryService;

    @GetMapping("/api/lore/categories/{id}")
    public TaxonomyCategoryResponse getById(@PathVariable Long id) {
        return taxonomyCategoryService.getById(id);
    }

    @GetMapping("/api/lore/categories/slug/{slug}")
    public TaxonomyCategoryResponse getBySlug(@PathVariable String slug) {
        return taxonomyCategoryService.getBySlug(slug);
    }

    @PutMapping("/api/lore/categories/{id}")
    public TaxonomyCategoryResponse update(@PathVariable Long id, @Valid @RequestBody TaxonomyCategoryRequest request) {
        return taxonomyCategoryService.update(id, request);
    }

    @DeleteMapping("/api/lore/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taxonomyCategoryService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/lore/categories")
    public List<TaxonomyCategoryResponse> listForMovie(@PathVariable Long movieId) {
        return taxonomyCategoryService.listForMovie(movieId);
    }

    @PostMapping("/api/movies/{movieId}/lore/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxonomyCategoryResponse addToMovie(@PathVariable Long movieId,
                                                @Valid @RequestBody TaxonomyCategoryRequest request) {
        return taxonomyCategoryService.addToMovie(movieId, request);
    }

    @GetMapping("/api/series/{seriesId}/lore/categories")
    public List<TaxonomyCategoryResponse> listForSeries(@PathVariable Long seriesId) {
        return taxonomyCategoryService.listForSeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/lore/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxonomyCategoryResponse addToSeries(@PathVariable Long seriesId,
                                                 @Valid @RequestBody TaxonomyCategoryRequest request) {
        return taxonomyCategoryService.addToSeries(seriesId, request);
    }
}
