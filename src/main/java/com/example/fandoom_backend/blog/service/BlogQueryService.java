package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogFilterCriteria;
import com.example.fandoom_backend.blog.dto.BlogFilterableSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogHubFacetsResponse;
import com.example.fandoom_backend.blog.dto.BlogSearchResult;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Blog hub'ın çok-facetli filtre+sıralama sorgusu, mevcut BlogService'in
// CRUD+ilişki-yönetimi sorumluluğundan kasıtlı olarak ayrı bir arayüzde
// (ISP): farklı bir istemci (public hub) yalnızca bu tek metodu kullanır,
// BlogService'in create/update/delete/replaceRelated gibi admin-amaçlı
// metodlarına bağımlı olmak zorunda kalmaz.
public interface BlogQueryService {
    PageResponse<BlogFilterableSummaryResponse> findFilterable(
            BlogFilterCriteria criteria, String sortKey, Pageable pageable);

    // Filtre panelinin seçenek+sayaç listesi. findFilterable'dan bağımsız
    // ayrı bir sorgu; biri sonuç listesini, diğeri seçenekleri döner.
    BlogHubFacetsResponse getFacets();
    // search/ modülünün quick search kapısı (MovieService.quickSearch ile aynı sözleşme). Yalnız PUBLISHED.
    List<BlogSearchResult> quickSearch(String query, int limit);
    // search/ modülünün full search kapısı: quickSearch ile aynı eşleşme/sıra, sayfalı. Yalnız PUBLISHED.
    // pageable unsorted olmalı.
    PageResponse<BlogSummaryResponse> fullSearch(String query, Pageable pageable);
}
