package com.example.fandoom_backend.franchise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FranchiseRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 500) String bannerImageUrl) {
}
