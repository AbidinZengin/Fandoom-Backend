package com.example.fandoom_backend.contentdrop.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropDetailResponse;
import com.example.fandoom_backend.contentdrop.dto.ContentDropRequest;
import com.example.fandoom_backend.contentdrop.dto.ContentDropSummaryResponse;
import com.example.fandoom_backend.contentdrop.entity.SubjectType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContentDropService {

    PageResponse<ContentDropSummaryResponse> list(Pageable pageable);

    // Admin/düzenleme amaçlı: durumdan bağımsız (DRAFT dahil).
    ContentDropDetailResponse getById(Long id);

    // Public: yalnızca PUBLISHED, viewCount'u artırır.
    ContentDropDetailResponse getBySlug(String slug);

    // Bölüm sayfası "Dive Deeper" carousel'i için [content-drops-veri] merdiveni.
    List<ContentDropSummaryResponse> findRelatedForProduction(
            SubjectType productionType, String productionSlug,
            Integer seasonNumber, Integer episodeNumber, int limit);

    ContentDropDetailResponse create(ContentDropRequest request);

    ContentDropDetailResponse update(Long id, ContentDropRequest request);

    void delete(Long id);

    boolean existsById(Long id);

    // Content drop sayfasının altındaki editöryel "ilgili içerikler" seçimini
    // (sıralı) tamamen değiştirir.
    List<ContentDropSummaryResponse> replaceRelated(Long id, List<Long> relatedContentDropIds);
}
