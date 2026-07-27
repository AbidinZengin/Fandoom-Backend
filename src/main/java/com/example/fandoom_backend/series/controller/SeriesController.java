package com.example.fandoom_backend.series.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesRequest;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.service.SeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public PageResponse<SeriesSummaryResponse> list(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long franchiseId,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return seriesService.search(search, pageable);
        }
        if (genreId != null) {
            return seriesService.listByGenre(genreId, pageable);
        }
        return franchiseId == null
                ? seriesService.list(pageable)
                : seriesService.listByFranchise(franchiseId, pageable);
    }

    @GetMapping("/{id}")
    public SeriesDetailResponse getById(@PathVariable Long id) {
        return seriesService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public SeriesDetailResponse getBySlug(@PathVariable String slug) {
        return seriesService.getBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeriesDetailResponse create(@Valid @RequestBody SeriesRequest request) {
        return seriesService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeriesDetailResponse> createBatch(@RequestBody @Valid List<@Valid SeriesRequest> requests) {
        return seriesService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public SeriesDetailResponse update(@PathVariable Long id, @Valid @RequestBody SeriesRequest request) {
        return seriesService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        seriesService.delete(id);
    }
}
