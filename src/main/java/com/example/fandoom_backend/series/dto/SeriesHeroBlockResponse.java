package com.example.fandoom_backend.series.dto;

import com.example.fandoom_backend.series.entity.HeroFontFamily;
import com.example.fandoom_backend.series.entity.RadiusToken;
import com.example.fandoom_backend.series.entity.SeriesHeroBlockType;

public record SeriesHeroBlockResponse(
        Long id, int orderIndex, SeriesHeroBlockType blockType,
        Double x, Double y, Double width, Double height,
        String imageUrl, Integer blurAmount,
        String text, String textTr,
        String backgroundColor, RadiusToken borderRadius,
        HeroFontFamily fontFamily, Double fontScale) {
}
