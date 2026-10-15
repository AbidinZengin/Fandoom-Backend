package com.example.fandoom_backend.search.service;

import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.PortalSummaryResponse;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.search.dto.QuickSearchResponse;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;

// q kuralları tüm metodlarda aynı: kırpılır; 2 karakterden kısaysa boş sonuç (DB'ye gidilmez), 100'den uzunsa
// InvalidReferenceException (400).
public interface SearchService {
    // Header arama kutusu: 4 türde paralel arama, tür başına en fazla 5 sonuç. Zamanında dönmeyen tür boş kalır.
    QuickSearchResponse quickSearch(String query);

    // Full search: tür başına sayfalı arama, mevcut summary DTO'larıyla. page < 0 -> 0, size 1..50'ye sıkıştırılır.
    // Cache'lenmez. Sıralama sabit (önce "ile başlayan", sonra alfabetik); client sort'u kabul edilmez.
    PageResponse<MovieSummaryResponse> searchMovies(String query, int page, int size);
    PageResponse<SeriesSummaryResponse> searchSeries(String query, int page, int size);
    PageResponse<PortalSummaryResponse> searchPortals(String query, Long viewerId, int page, int size);
    PageResponse<BlogSummaryResponse> searchBlogs(String query, int page, int size);
}
