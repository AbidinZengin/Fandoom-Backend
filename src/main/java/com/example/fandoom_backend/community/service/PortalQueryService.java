package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Public portal okuma (dizin + detay). HIDDEN hiçbir yerde görünmez; ACTIVE ve ARCHIVED listelenir.
public interface PortalQueryService {

    // sort: trending (varsayılan) | members | new | alpha; geçersiz değer trending sayılır. q: TR+EN ad araması
    // (LIKE wildcard'ları escape edilir, en fazla 100 karakter -> aksi 400). viewerId null (anonim) -> isMember=false.
    PageResponse<PortalSummaryResponse> list(String sort, String q, Long viewerId, Pageable pageable);

    // Portallarım: kullanıcının üye olduğu portallar, düz List (sayfalama yok). sort=null -> joinedAt azalan;
    // "activity" -> trending skoruna (bkz. PortalQueryServiceImpl) göre azalan, eşitlikte joinedAt azalan.
    // ARCHIVED görünür, HIDDEN görünmez. Hepsinde isMember=true.
    List<PortalSummaryResponse> listJoined(Long userId, String sort);

    // HIDDEN/yok -> 404.
    PortalDetailResponse getBySlug(String slug, Long viewerId);
}
