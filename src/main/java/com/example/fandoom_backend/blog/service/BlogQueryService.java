package com.example.fandoom_backend.blog.service;

import com.example.fandoom_backend.blog.dto.BlogFilterCriteria;
import com.example.fandoom_backend.blog.dto.BlogFilterableSummaryResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

// Blog hub'ın çok-facetli filtre+sıralama sorgusu, mevcut BlogService'in
// CRUD+ilişki-yönetimi sorumluluğundan kasıtlı olarak ayrı bir arayüzde
// (ISP): farklı bir istemci (public hub) yalnızca bu tek metodu kullanır,
// BlogService'in create/update/delete/replaceRelated gibi admin-amaçlı
// metodlarına bağımlı olmak zorunda kalmaz.
public interface BlogQueryService {
    PageResponse<BlogFilterableSummaryResponse> findFilterable(
            BlogFilterCriteria criteria, String sortKey, Pageable pageable);
}
