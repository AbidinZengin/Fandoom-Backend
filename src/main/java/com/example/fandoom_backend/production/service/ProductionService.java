package com.example.fandoom_backend.production.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface ProductionService {
    PageResponse<ProductionSummaryResponse> list(Pageable pageable);
}
