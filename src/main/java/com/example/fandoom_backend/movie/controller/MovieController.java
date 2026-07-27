package com.example.fandoom_backend.movie.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.movie.dto.MovieDetailResponse;
import com.example.fandoom_backend.movie.dto.MovieRequest;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.movie.service.MovieService;
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
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public PageResponse<MovieSummaryResponse> list(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long franchiseId,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return movieService.search(search, pageable);
        }
        if (genreId != null) {
            return movieService.listByGenre(genreId, pageable);
        }
        return franchiseId == null
                ? movieService.list(pageable)
                : movieService.listByFranchise(franchiseId, pageable);
    }

    @GetMapping("/{id}")
    public MovieDetailResponse getById(@PathVariable Long id) {
        return movieService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public MovieDetailResponse getBySlug(@PathVariable String slug) {
        return movieService.getBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieDetailResponse create(@Valid @RequestBody MovieRequest request) {
        return movieService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MovieDetailResponse> createBatch(@RequestBody @Valid List<@Valid MovieRequest> requests) {
        return movieService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public MovieDetailResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        movieService.delete(id);
    }
}
