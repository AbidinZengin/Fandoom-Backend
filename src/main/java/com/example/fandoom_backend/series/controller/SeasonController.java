package com.example.fandoom_backend.series.controller;

import com.example.fandoom_backend.series.dto.SeasonDetailResponse;
import com.example.fandoom_backend.series.dto.SeasonRequest;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.service.SeasonService;
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
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping("/api/series/{seriesId}/seasons")
    public List<SeasonSummaryResponse> listBySeries(@PathVariable Long seriesId) {
        return seasonService.listBySeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public SeasonDetailResponse create(@PathVariable Long seriesId, @Valid @RequestBody SeasonRequest request) {
        return seasonService.create(seriesId, request);
    }

    @PostMapping("/api/series/{seriesId}/seasons/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeasonDetailResponse> createBatch(@PathVariable Long seriesId, @RequestBody @Valid List<@Valid SeasonRequest> requests) {
        return seasonService.createBatch(seriesId, requests);
    }

    @GetMapping("/api/seasons/{id}")
    public SeasonDetailResponse getById(@PathVariable Long id) {
        return seasonService.getById(id);
    }

    @PutMapping("/api/seasons/{id}")
    public SeasonDetailResponse update(@PathVariable Long id, @Valid @RequestBody SeasonRequest request) {
        return seasonService.update(id, request);
    }

    @DeleteMapping("/api/seasons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        seasonService.delete(id);
    }
}
