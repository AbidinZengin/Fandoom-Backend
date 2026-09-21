package com.example.fandoom_backend.community.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalCreateRequest;
import com.example.fandoom_backend.community.dto.PortalUpdateRequest;
import com.example.fandoom_backend.community.service.PortalAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Yalnız ADMIN (SecurityConfig: /api/admin/community/**). Silme ucu YOK — kaldırmak = archive/HIDDEN.
@RestController
@RequestMapping("/api/admin/community/portals")
@RequiredArgsConstructor
public class PortalAdminController {

    private final PortalAdminService portalAdminService;

    @GetMapping
    public PageResponse<AdminPortalResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return portalAdminService.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPortalResponse create(@Valid @RequestBody PortalCreateRequest request) {
        return portalAdminService.create(request);
    }

    @PatchMapping("/{slug}")
    public AdminPortalResponse update(@PathVariable String slug, @Valid @RequestBody PortalUpdateRequest request) {
        return portalAdminService.update(slug, request);
    }

    @PostMapping("/{slug}/archive")
    public AdminPortalResponse archive(@PathVariable String slug) {
        return portalAdminService.archive(slug);
    }

    @PostMapping("/{slug}/unarchive")
    public AdminPortalResponse unarchive(@PathVariable String slug) {
        return portalAdminService.unarchive(slug);
    }
}
