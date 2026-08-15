package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.HeroFontFamily;
import com.example.fandoom_backend.series.entity.RadiusToken;
import com.example.fandoom_backend.series.entity.SeriesHeroBlockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SeriesHeroBlockRequest(
        @NotNull SeriesHeroBlockType blockType,
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        Double height,
        @Size(max = 500) String imageUrl,
        @PositiveOrZero Integer blurAmount,
        @Size(max = 5000) String textTr,
        @Size(max = 5000) String text,
        @Size(max = 20) String backgroundColor,
        RadiusToken borderRadius,
        HeroFontFamily fontFamily,
        Double fontScale) {
}
