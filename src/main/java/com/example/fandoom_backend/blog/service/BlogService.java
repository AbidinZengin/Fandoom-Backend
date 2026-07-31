package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogRequest;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.entity.SubjectType;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BlogService {

    PageResponse<BlogSummaryResponse> list(Pageable pageable);

    // Admin/düzenleme amaçlı: durumdan bağımsız (DRAFT dahil).
    BlogDetailResponse getById(Long id);

    // Public: yalnızca PUBLISHED, viewCount'u artırır.
    BlogDetailResponse getBySlug(String slug);

    // Bölüm sayfası "Dive Deeper" carousel'i için [blog-veri] merdiveni.
    List<BlogSummaryResponse> findRelatedForProduction(
            SubjectType productionType, String productionSlug,
            Integer seasonNumber, Integer episodeNumber, int limit);

    BlogDetailResponse create(BlogRequest request);

    BlogDetailResponse update(Long id, BlogRequest request);

    void delete(Long id);

    boolean existsById(Long id);

    // Blog sayfasının altındaki editöryel "ilgili içerikler" seçimini
    // (sıralı) tamamen değiştirir.
    List<BlogSummaryResponse> replaceRelated(Long id, List<Long> relatedBlogIds);
}
