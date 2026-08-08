package com.example.fandoom_backend.cms.controller;

import com.example.fandoom_backend.cms.dto.HomeBlockRequest;
import com.example.fandoom_backend.cms.dto.HomeBlockResponse;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.service.HomeBlockService;
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

    private final HomeBlockService homeBlockService;

    @GetMapping("/pages/{pageName}")
    public List<HomeBlockResponse> getByPage(
            @PathVariable PageName pageName,
            @RequestParam(required = false) Long entityId) {
        return homeBlockService.getByPage(pageName, entityId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public HomeBlockResponse create(@Valid @RequestBody HomeBlockRequest request) {
        return homeBlockService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HomeBlockResponse update(@PathVariable Long id, @Valid @RequestBody HomeBlockRequest request) {
        return homeBlockService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        homeBlockService.delete(id);
    }
}
