package com.example.fandoom_backend.franchise.mapper;

import com.example.fandoom_backend.franchise.dto.FranchiseDetailResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseSummaryResponse;
import com.example.fandoom_backend.franchise.entity.Franchise;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FranchiseMapper {
    FranchiseSummaryResponse toSummaryResponse(Franchise franchise);
    FranchiseDetailResponse toDetailResponse(Franchise franchise);
    List<FranchiseSummaryResponse> toSummaryResponseList(List<Franchise> franchises);
}
