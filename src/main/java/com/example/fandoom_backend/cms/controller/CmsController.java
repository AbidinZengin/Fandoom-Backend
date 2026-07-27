package com.example.fandoom_backend.cms.controller;

import com.example.fandoom_backend.cms.dto.PageContentRequest;
import com.example.fandoom_backend.cms.dto.PageContentResponse;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.service.PageContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Yazma uçları (POST/PUT/DELETE) ADMIN gerektirir — path-bazlı kural (bkz.
// common/config/SecurityConfig, birincil/otoriter) ve aşağıdaki @PreAuthorize
// (savunma derinliği).
@RestController
@RequestMapping("/api/cms")
@RequiredArgsConstructor
public class CmsController {

    private final PageContentService pageContentService;

    @GetMapping("/pages/{pageName}")
    public List<PageContentResponse> getByPage(
            @PathVariable PageName pageName,
            @RequestParam(required = false) Long entityId) {
        return pageContentService.getByPage(pageName, entityId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PageContentResponse create(@Valid @RequestBody PageContentRequest request) {
        return pageContentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PageContentResponse update(@PathVariable Long id, @Valid @RequestBody PageContentRequest request) {
        return pageContentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        pageContentService.delete(id);
    }
}
