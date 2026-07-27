package com.example.fandoom_backend.production.service;

import com.example.fandoom_backend.common.dto.CursorPageResponse;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;

import java.time.LocalDate;

public interface ProductionService {
    CursorPageResponse<ProductionSummaryResponse> list(LocalDate cursorDate, Long cursorId, int size);
}
