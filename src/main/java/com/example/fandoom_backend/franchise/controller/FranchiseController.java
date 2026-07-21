package com.example.fandoom_backend.franchise.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseDetailResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseRequest;
import com.example.fandoom_backend.franchise.dto.FranchiseSummaryResponse;
import com.example.fandoom_backend.franchise.service.FranchiseService;
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
@RequestMapping("/api/franchises")
@RequiredArgsConstructor
public class FranchiseController {

    private final FranchiseService franchiseService;

    @GetMapping
    public PageResponse<FranchiseSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return franchiseService.list(pageable);
    }

    @GetMapping("/{id}")
    public FranchiseDetailResponse getById(@PathVariable Long id) {
        return franchiseService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public FranchiseDetailResponse getBySlug(@PathVariable String slug) {
        return franchiseService.getBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FranchiseDetailResponse create(@Valid @RequestBody FranchiseRequest request) {
        return franchiseService.create(request);
    }

    @PutMapping("/{id}")
    public FranchiseDetailResponse update(@PathVariable Long id, @Valid @RequestBody FranchiseRequest request) {
        return franchiseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        franchiseService.delete(id);
    }
}
