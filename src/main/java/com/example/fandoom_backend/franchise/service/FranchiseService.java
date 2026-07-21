package com.example.fandoom_backend.franchise.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseDetailResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseRequest;
import com.example.fandoom_backend.franchise.dto.FranchiseSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface FranchiseService {
    PageResponse<FranchiseSummaryResponse> list(Pageable pageable);
    FranchiseDetailResponse getById(Long id);
    FranchiseDetailResponse getBySlug(String slug);
    FranchiseDetailResponse create(FranchiseRequest request);
    FranchiseDetailResponse update(Long id, FranchiseRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
