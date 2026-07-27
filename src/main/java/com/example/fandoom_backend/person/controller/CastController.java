package com.example.fandoom_backend.person.controller;

import com.example.fandoom_backend.person.dto.CastRequest;
import com.example.fandoom_backend.person.dto.CastResponse;
import com.example.fandoom_backend.person.service.CastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CastController {

    private final CastService castService;

    @GetMapping("/api/movies/{movieId}/cast")
    public List<CastResponse> listForMovie(@PathVariable Long movieId) {
        return castService.listForMovie(movieId);
    }

    @PostMapping("/api/movies/{movieId}/cast")
    @ResponseStatus(HttpStatus.CREATED)
    public CastResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody CastRequest request) {
        return castService.addToMovie(movieId, request);
    }

    @PostMapping("/api/movies/{movieId}/cast/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CastResponse> addToMovieBatch(@PathVariable Long movieId, @RequestBody @Valid List<@Valid CastRequest> requests) {
        return castService.addToMovieBatch(movieId, requests);
    }

    @GetMapping("/api/series/{seriesId}/cast")
    public List<CastResponse> listForSeries(@PathVariable Long seriesId) {
        return castService.listForSeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/cast")
    @ResponseStatus(HttpStatus.CREATED)
    public CastResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody CastRequest request) {
        return castService.addToSeries(seriesId, request);
    }

    @PostMapping("/api/series/{seriesId}/cast/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CastResponse> addToSeriesBatch(@PathVariable Long seriesId, @RequestBody @Valid List<@Valid CastRequest> requests) {
        return castService.addToSeriesBatch(seriesId, requests);
    }

    @DeleteMapping("/api/cast/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        castService.delete(id);
    }
}
