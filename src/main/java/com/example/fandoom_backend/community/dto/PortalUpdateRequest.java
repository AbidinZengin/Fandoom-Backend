package com.example.fandoom_backend.community.dto;

import com.example.fandoom_backend.community.entity.PortalPostingPolicy;
import com.example.fandoom_backend.community.entity.PortalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// PATCH: null = "değişmedi". slug alanı yalnızca "değiştirilemez" kuralını açıkça reddetmek için var
// (mevcut slug'dan farklı bir değer gönderilirse 400; aynı değer kabul edilir). Görsel URL'lerinde boş string =
// görseli kaldır. accentColor: null = değişmedi, "" = rengi kaldır, dolu = #RRGGBB. productionSlugs: null = değişmedi, [] = tüm yapım bağları silinir, dolu = tamamen değiştirir.
public record PortalUpdateRequest(
        String slug,
        @Size(min = 2, max = 60) @Pattern(regexp = NOT_BLANK) String nameTr,
        @Size(min = 2, max = 60) @Pattern(regexp = NOT_BLANK) String nameEn,
        @Size(max = 300) @Pattern(regexp = NOT_WHITESPACE_ONLY) String descriptionTr,
        @Size(max = 300) @Pattern(regexp = NOT_WHITESPACE_ONLY) String descriptionEn,
        @Size(max = 500) String bannerUrl,
        @Size(max = 500) String iconUrl,
        PortalPostingPolicy postingPolicy,
        PortalStatus status,
        @Size(max = 50) List<@NotBlank @Size(max = 280) String> productionSlugs,
        Integer sortOrder,
        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$") String accentColor) {

    // İsimler: en az bir boşluk-dışı karakter ("   " geçersiz; create'teki @NotBlank ile tutarlı). null = değişmedi.
    static final String NOT_BLANK = "(?s).*\\S.*";
    // Açıklamalar: "" hâlâ "temizle" demektir, ama yalnız boşluktan oluşan ("   ") değer geçersiz. null = değişmedi.
    static final String NOT_WHITESPACE_ONLY = "(?s)^(?!\\s+$).*$";
}
