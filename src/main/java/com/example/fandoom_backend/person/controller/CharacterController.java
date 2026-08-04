package com.example.fandoom_backend.person.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import com.example.fandoom_backend.person.service.CharacterService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping("/api/characters")
    public PageResponse<CharacterResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return characterService.list(pageable);
    }

    @GetMapping("/api/characters/{id}")
    public CharacterResponse getById(@PathVariable Long id) {
        return characterService.getById(id);
    }

    @GetMapping("/api/characters/slug/{slug}")
    public CharacterResponse getBySlug(@PathVariable String slug) {
        return characterService.getBySlug(slug);
    }

    @PutMapping("/api/characters/{id}")
    public CharacterResponse update(@PathVariable Long id, @Valid @RequestBody CharacterRequest request) {
        return characterService.update(id, request);
    }

    @DeleteMapping("/api/characters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        characterService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/characters")
    public List<CharacterResponse> listForMovie(@PathVariable Long movieId) {
        return characterService.listForMovie(movieId);
    }

    @PostMapping("/api/movies/{movieId}/characters")
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody CharacterRequest request) {
        return characterService.addToMovie(movieId, request);
    }

    @PostMapping("/api/movies/{movieId}/characters/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CharacterResponse> addToMovieBatch(@PathVariable Long movieId, @RequestBody @Valid List<@Valid CharacterRequest> requests) {
        return characterService.addToMovieBatch(movieId, requests);
    }

    @GetMapping("/api/series/{seriesId}/characters")
    public List<CharacterResponse> listForSeries(@PathVariable Long seriesId) {
        return characterService.listForSeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/characters")
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody CharacterRequest request) {
        return characterService.addToSeries(seriesId, request);
    }

    @PostMapping("/api/series/{seriesId}/characters/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CharacterResponse> addToSeriesBatch(@PathVariable Long seriesId, @RequestBody @Valid List<@Valid CharacterRequest> requests) {
        return characterService.addToSeriesBatch(seriesId, requests);
    }
}
