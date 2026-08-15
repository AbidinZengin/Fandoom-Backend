package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.LocationRequest;
import com.example.fandoom_backend.lore.dto.LocationResponse;
import com.example.fandoom_backend.lore.service.LocationService;
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
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/api/lore/locations/{id}")
    public LocationResponse getById(@PathVariable Long id) {
        return locationService.getById(id);
    }

    @GetMapping("/api/lore/locations/slug/{slug}")
    public LocationResponse getBySlug(@PathVariable String slug) {
        return locationService.getBySlug(slug);
    }

    @PutMapping("/api/lore/locations/{id}")
    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(id, request);
    }

    @DeleteMapping("/api/lore/locations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        locationService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/lore/locations")
    public List<LocationResponse> listForMovie(@PathVariable Long movieId) {
        return locationService.listForMovie(movieId);
    }

    @PostMapping("/api/movies/{movieId}/lore/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody LocationRequest request) {
        return locationService.addToMovie(movieId, request);
    }

    @GetMapping("/api/series/{seriesId}/lore/locations")
    public List<LocationResponse> listForSeries(@PathVariable Long seriesId) {
        return locationService.listForSeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/lore/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody LocationRequest request) {
        return locationService.addToSeries(seriesId, request);
    }
}
