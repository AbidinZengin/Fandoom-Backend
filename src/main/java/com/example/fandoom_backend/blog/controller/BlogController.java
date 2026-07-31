package com.example.fandoom_backend.blog.controller;

import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogRequest;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.dto.ReplaceRelatedRequest;
import com.example.fandoom_backend.blog.entity.SubjectType;
import com.example.fandoom_backend.blog.service.BlogService;
import com.example.fandoom_backend.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    public PageResponse<BlogSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return blogService.list(pageable);
    }

    @GetMapping("/{id}")
    public BlogDetailResponse getById(@PathVariable Long id) {
        return blogService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public BlogDetailResponse getBySlug(@PathVariable String slug) {
        return blogService.getBySlug(slug);
    }

    // Bölüm sayfasının "Dive Deeper" carousel'i buradan besleniyor — frontend'in
    // RelatedContent.data.js'te işaret ettiği gelecekteki sözleşmenin (GET
    // /api/content?productionSlug=&seasonNumber=&episodeNumber=) blogs'a
    // özelleşmiş hâli.
    @GetMapping("/related")
    public List<BlogSummaryResponse> findRelated(
            @RequestParam SubjectType productionType,
            @RequestParam String productionSlug,
            @RequestParam(required = false) Integer seasonNumber,
            @RequestParam(required = false) Integer episodeNumber,
            @RequestParam(defaultValue = "9") int limit) {
        return blogService.findRelatedForProduction(
                productionType, productionSlug, seasonNumber, episodeNumber, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlogDetailResponse create(@Valid @RequestBody BlogRequest request) {
        return blogService.create(request);
    }

    @PutMapping("/{id}")
    public BlogDetailResponse update(@PathVariable Long id, @Valid @RequestBody BlogRequest request) {
        return blogService.update(id, request);
    }

    @PutMapping("/{id}/related")
    public List<BlogSummaryResponse> replaceRelated(
            @PathVariable Long id, @Valid @RequestBody ReplaceRelatedRequest request) {
        return blogService.replaceRelated(id, request.relatedBlogIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        blogService.delete(id);
    }
}
