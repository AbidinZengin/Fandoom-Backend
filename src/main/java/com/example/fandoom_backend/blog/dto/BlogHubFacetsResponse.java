package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.tag.dto.TagFacetOptionResponse;

import java.util.List;

// Blog hub filtre panelinin tek seferde ihtiyaç duyduğu tüm facet
// seçenekleri (sayaçlarıyla birlikte). GET /api/blogs/hub/facets bunu
// döner; asıl listeleme (GET /api/blogs/hub) bu endpoint'ten tamamen
// bağımsızdır — biri filtre seçeneklerini, diğeri filtrelenmiş sonucu verir.
public record BlogHubFacetsResponse(
        List<TagFacetOptionResponse> formats,
        List<TagFacetOptionResponse> moods,
        List<TagFacetOptionResponse> themes,
        List<FranchiseFacetOptionResponse> franchises) {
}
