package com.example.fandoom_backend.search.controller;

import com.example.fandoom_backend.search.dto.QuickSearchResponse;
import com.example.fandoom_backend.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Sözleşme: docs/search-api-contract.md. Public (SecurityConfig: GET /api/search/** permitAll).
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/quick")
    public QuickSearchResponse quick(@RequestParam("q") String q) {
        return searchService.quickSearch(q);
    }
}
