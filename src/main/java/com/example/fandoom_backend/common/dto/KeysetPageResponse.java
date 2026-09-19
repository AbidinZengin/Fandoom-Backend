package com.example.fandoom_backend.common.dto;

import java.util.List;

// Keyset (cursor) sayfalama zarfı: COUNT(*) ve OFFSET yok. nextCursor opak bir
// string'tir (bkz. KeysetCursor), client bir sonraki istekte aynen geri yollar.
public record KeysetPageResponse<T>(List<T> content, boolean hasNext, String nextCursor) {}
