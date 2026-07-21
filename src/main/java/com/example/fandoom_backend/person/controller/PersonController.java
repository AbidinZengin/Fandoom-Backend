package com.example.fandoom_backend.person.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.PersonDetailResponse;
import com.example.fandoom_backend.person.dto.PersonRequest;
import com.example.fandoom_backend.person.dto.PersonSummaryResponse;
import com.example.fandoom_backend.person.service.PersonService;
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

@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping
    public PageResponse<PersonSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return personService.list(pageable);
    }

    @GetMapping("/{id}")
    public PersonDetailResponse getById(@PathVariable Long id) {
        return personService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public PersonDetailResponse getBySlug(@PathVariable String slug) {
        return personService.getBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonDetailResponse create(@Valid @RequestBody PersonRequest request) {
        return personService.create(request);
    }

    @PutMapping("/{id}")
    public PersonDetailResponse update(@PathVariable Long id, @Valid @RequestBody PersonRequest request) {
        return personService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        personService.delete(id);
    }
}
