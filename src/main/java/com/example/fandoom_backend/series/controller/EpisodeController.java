package com.example.fandoom_backend.series.controller;

import com.example.fandoom_backend.series.dto.EpisodeRequest;
import com.example.fandoom_backend.series.dto.EpisodeResponse;
import com.example.fandoom_backend.series.service.EpisodeService;
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
public class EpisodeController {

    private final EpisodeService episodeService;

    @GetMapping("/api/seasons/{seasonId}/episodes")
    public List<EpisodeResponse> listBySeason(@PathVariable Long seasonId) {
        return episodeService.listBySeason(seasonId);
    }

    @PostMapping("/api/seasons/{seasonId}/episodes")
    @ResponseStatus(HttpStatus.CREATED)
    public EpisodeResponse create(@PathVariable Long seasonId, @Valid @RequestBody EpisodeRequest request) {
        return episodeService.create(seasonId, request);
    }

    @PostMapping("/api/seasons/{seasonId}/episodes/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EpisodeResponse> createBatch(@PathVariable Long seasonId, @RequestBody @Valid List<@Valid EpisodeRequest> requests) {
        return episodeService.createBatch(seasonId, requests);
    }

    @GetMapping("/api/episodes/{id}")
    public EpisodeResponse getById(@PathVariable Long id) {
        return episodeService.getById(id);
    }

    @PutMapping("/api/episodes/{id}")
    public EpisodeResponse update(@PathVariable Long id, @Valid @RequestBody EpisodeRequest request) {
        return episodeService.update(id, request);
    }

    @DeleteMapping("/api/episodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        episodeService.delete(id);
    }
}
