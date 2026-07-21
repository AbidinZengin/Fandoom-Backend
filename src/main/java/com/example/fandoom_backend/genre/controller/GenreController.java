package com.example.fandoom_backend.genre.controller;

import com.example.fandoom_backend.genre.dto.GenreRequest;
import com.example.fandoom_backend.genre.dto.GenreResponse;
import com.example.fandoom_backend.genre.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    @GetMapping
    public List<GenreResponse> list() {
        return genreService.list();
    }

    @GetMapping("/{id}")
    public GenreResponse getById(@PathVariable Long id) {
        return genreService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenreResponse create(@Valid @RequestBody GenreRequest request) {
        return genreService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GenreResponse> createBatch(@RequestBody @Valid List<@Valid GenreRequest> requests) {
        return genreService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public GenreResponse update(@PathVariable Long id, @Valid @RequestBody GenreRequest request) {
        return genreService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        genreService.delete(id);
    }
}
