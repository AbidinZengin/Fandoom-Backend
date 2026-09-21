package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.AdminPortalResponse;
import com.example.fandoom_backend.community.dto.PortalCreateRequest;
import com.example.fandoom_backend.community.dto.PortalUpdateRequest;
import org.springframework.data.domain.Pageable;

// Yalnız ADMIN (SecurityConfig: /api/admin/community/**). Silme YOK — kaldırmak = archive (veya status=HIDDEN).
public interface PortalAdminService {

    // HIDDEN dahil tüm portallar, sortOrder ASC + id ASC.
    PageResponse<AdminPortalResponse> list(Pageable pageable);

    AdminPortalResponse create(PortalCreateRequest request);

    // slug HARİÇ tüm alanlar; null = değişmedi.
    AdminPortalResponse update(String slug, PortalUpdateRequest request);

    AdminPortalResponse archive(String slug);

    AdminPortalResponse unarchive(String slug);
}
