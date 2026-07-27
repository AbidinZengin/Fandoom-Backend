package com.example.fandoom_backend.production.controller;

import com.example.fandoom_backend.common.dto.CursorPageResponse;
import com.example.fandoom_backend.production.dto.ProductionSummaryResponse;
import com.example.fandoom_backend.production.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping
    public CursorPageResponse<ProductionSummaryResponse> list(
            @RequestParam(required = false) LocalDate cursorDate,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size) {
        return productionService.list(cursorDate, cursorId, size);
    }
}
