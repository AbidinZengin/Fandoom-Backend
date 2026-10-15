package com.example.fandoom_backend.search.service;

import com.example.fandoom_backend.search.dto.QuickSearchResponse;

public interface SearchService {
    // Header arama kutusu: 4 türde paralel arama, tür başına en fazla 5 sonuç. q kırpılır; 2 karakterden kısaysa
    // boş yanıt (DB'ye gidilmez), 100'den uzunsa InvalidReferenceException (400). Zamanında dönmeyen tür boş kalır.
    QuickSearchResponse quickSearch(String query);
}
