package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.PortalDetailResponse;
import com.example.fandoom_backend.community.dto.PortalSearchResult;
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

    // search/ modülünün quick search kapısı (MovieService.quickSearch ile aynı sözleşme). Yalnız ACTIVE + ARCHIVED.
    List<PortalSearchResult> quickSearch(String query, int limit);
    // search/ modülünün full search kapısı: quickSearch ile aynı eşleşme/sıra, sayfalı. viewerId null -> isMember=false.
    // pageable unsorted olmalı.
    PageResponse<PortalSummaryResponse> fullSearch(String query, Long viewerId, Pageable pageable);
    // HIDDEN/yok -> 404.
    PortalDetailResponse getBySlug(String slug, Long viewerId);
}
