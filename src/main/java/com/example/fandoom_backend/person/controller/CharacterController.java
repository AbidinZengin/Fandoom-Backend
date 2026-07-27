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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public PageResponse<CharacterResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return characterService.list(pageable);
    }

    @GetMapping("/{id}")
    public CharacterResponse getById(@PathVariable Long id) {
        return characterService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public CharacterResponse getBySlug(@PathVariable String slug) {
        return characterService.getBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse create(@Valid @RequestBody CharacterRequest request) {
        return characterService.create(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CharacterResponse> createBatch(@RequestBody @Valid List<@Valid CharacterRequest> requests) {
        return characterService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public CharacterResponse update(@PathVariable Long id, @Valid @RequestBody CharacterRequest request) {
        return characterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        characterService.delete(id);
    }
}
