package com.example.fandoom_backend.search.controller;

import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.search.dto.QuickSearchResponse;
import com.example.fandoom_backend.search.service.SearchService;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Sözleşme: docs/search-api-contract.md. Public (SecurityConfig: GET /api/search/** permitAll).
// Full search tür başına ayrı uç: her uç tek ve belli bir tip döndürür (?type= ile tek uç PageResponse<?> olurdu).
// Sayfalama bilinçli olarak Pageable değil düz page/size: client'ın sort'u kabul edilmez, sıra sorguda sabit.
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/quick")
    public QuickSearchResponse quick(@RequestParam("q") String q) {
        return searchService.quickSearch(q);
    }

    @GetMapping("/movies")
    public PageResponse<MovieSummaryResponse> movies(@RequestParam("q") String q,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return searchService.searchMovies(q, page, size);
    }

    @GetMapping("/series")
    public PageResponse<SeriesSummaryResponse> series(@RequestParam("q") String q,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return searchService.searchSeries(q, page, size);
    }

    @GetMapping("/portals")
    public PageResponse<PortalSummaryResponse> portals(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails principal) {
        return searchService.searchPortals(q, principal == null ? null : principal.getId(), page, size);
    }

    @GetMapping("/blogs")
    public PageResponse<BlogSummaryResponse> blogs(@RequestParam("q") String q,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return searchService.searchBlogs(q, page, size);
    }
}
