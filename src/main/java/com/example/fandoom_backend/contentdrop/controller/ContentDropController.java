package com.example.fandoom_backend.contentdrop.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.dto.ReplaceRelatedRequest;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import com.example.fandoom_backend.contentdrop.service.ContentDropService;
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
@RequestMapping("/api/content-drops")
@RequiredArgsConstructor
public class ContentDropController {

    private final ContentDropService contentDropService;

    @GetMapping
    public PageResponse<ContentDropSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return contentDropService.list(pageable);
    }

    @GetMapping("/{id}")
    public ContentDropDetailResponse getById(@PathVariable Long id) {
        return contentDropService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public ContentDropDetailResponse getBySlug(@PathVariable String slug) {
        return contentDropService.getBySlug(slug);
    }

    // Bölüm sayfasının "Dive Deeper" carousel'i buradan besleniyor — frontend'in
    // RelatedContent.data.js'te işaret ettiği gelecekteki sözleşmenin (GET
    // /api/content?productionSlug=&seasonNumber=&episodeNumber=) content-drops'a
    // özelleşmiş hâli.
    @GetMapping("/related")
    public List<ContentDropSummaryResponse> findRelated(
            @RequestParam SubjectType productionType,
            @RequestParam String productionSlug,
            @RequestParam(required = false) Integer seasonNumber,
            @RequestParam(required = false) Integer episodeNumber,
            @RequestParam(defaultValue = "9") int limit) {
        return contentDropService.findRelatedForProduction(
                productionType, productionSlug, seasonNumber, episodeNumber, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentDropDetailResponse create(@Valid @RequestBody ContentDropRequest request) {
        return contentDropService.create(request);
    }

    @PutMapping("/{id}")
    public ContentDropDetailResponse update(@PathVariable Long id, @Valid @RequestBody ContentDropRequest request) {
        return contentDropService.update(id, request);
    }

    @PutMapping("/{id}/related")
    public List<ContentDropSummaryResponse> replaceRelated(
            @PathVariable Long id, @Valid @RequestBody ReplaceRelatedRequest request) {
        return contentDropService.replaceRelated(id, request.relatedContentDropIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contentDropService.delete(id);
    }
}
