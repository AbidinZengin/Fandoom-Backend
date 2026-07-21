package com.example.fandoom_backend.production.controller;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;
import com.example.fandoom_backend.production.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping
    public PageResponse<ProductionSummaryResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return productionService.list(pageable);
    }
}
