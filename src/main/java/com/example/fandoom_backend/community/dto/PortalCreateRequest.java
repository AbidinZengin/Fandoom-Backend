package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Admin oluşturma. slug DEĞİŞMEZ (oluşturulduktan sonra PATCH'te değiştirilemez). status/postingPolicy/
// sortOrder verilmezse ACTIVE/OPEN/0. productionSlugs: bağlanacak yapımlar (boş/null = yapımsız portal).
// accentColor: opsiyonel #RRGGBB (yalnız hex doğrulanır; gradyan/sunum FE'nin işi). Boş/null = renk yok.
// bannerUrl/iconUrl: yalnız URL saklanır (önce POST /api/media/images ile yüklenir, folder=portals).
public record PortalCreateRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String slug,
        @NotBlank @Size(min = 2, max = 60) String nameTr,
        @NotBlank @Size(min = 2, max = 60) String nameEn,
        @Size(max = 300) String descriptionTr,
        @Size(max = 300) String descriptionEn,
        @Size(max = 500) String bannerUrl,
        @Size(max = 500) String iconUrl,
        PortalPostingPolicy postingPolicy,
        PortalStatus status,
        @Size(max = 50) List<@NotBlank @Size(max = 280) String> productionSlugs,
        Integer sortOrder,
        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$") String accentColor) {
}
