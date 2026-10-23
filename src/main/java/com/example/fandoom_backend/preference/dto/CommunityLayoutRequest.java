package com.example.fandoom_backend.preference.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// PUT tam yazar: tüm alanlar zorunlu. Modlar String + @Pattern: bilinmeyen değer fieldErrors ile 400 olsun
// (enum + @JsonCreator HttpMessageNotReadable'a düşüp alan bilgisi kaybolurdu).
public record CommunityLayoutRequest(
        @NotNull @Min(200) @Max(420) Integer leftWidth,
        @NotNull @Min(240) @Max(420) Integer rightWidth,
        @NotNull @Pattern(regexp = "full|rail|hidden", message = "full, rail veya hidden olmalı") String leftMode,
        @NotNull @Pattern(regexp = "full|hidden", message = "full veya hidden olmalı") String rightMode) {
}
