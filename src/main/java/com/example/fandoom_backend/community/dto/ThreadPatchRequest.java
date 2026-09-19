package com.example.fandoom_backend.community.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Tüm alanlar opsiyonel/kısmi güncelleme — null = "değişmedi"
// (UpdateUserProfileRequest deseni, bkz. account/). tags: null=korunur,
// []=tüm tag'ler silinir (BlogServiceImpl.applyTags deseniyle tutarlı).
// media aynı desen: null=değişmedi, []=hepsi silinir, dolu liste=tamamen değiştirir.
// imageUrl DEPRECATED (yalnızca media null iken): dolu url -> media[IMAGE url], boş string -> media [].
public record ThreadPatchRequest(
        @Size(min = 10, max = 200) String title,
        @Size(max = 10000) String body,
        @Size(max = 500) String imageUrl,
        Boolean spoilerFlagged,
        @Size(max = 10) List<@NotBlank @Size(max = 100) String> tags,
        @Size(max = 6) List<@Valid @NotNull ThreadMediaRequest> media) {
}
