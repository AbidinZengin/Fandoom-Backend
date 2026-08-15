package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// Series detay sayfasının Hero bileşeni: id/orderIndex ContentBlock'tan
// miras, col/row KULLANILMIYOR (blog/BlogBlock ile aynı sebep — serbest
// kanvas konumlama x/y/width/height burada tutulur). Alanların çoğu
// blockType'a göre koşullu doludur (bkz. SeriesHeroBlockType):
// TITLE/META/SYNOPSIS/BUTTON hiç içerik alanı taşımaz, render anında
// Series'in kendi alanlarından (titleTr/title, synopsisTr/synopsis, trailerUrl)
// otomatik beslenir. "Ekle/çıkar" = bu tabloda CRUD (bulk replace, bkz.
// SeriesServiceImpl.applyHeroBlocks).
// @Builder yalnızca burada tanımlı alanları kapsar — orderIndex build()
// sonrası setter ile atanır (BlogBlock ile aynı desen).
@Entity
@Table(name = "series_hero_block", indexes = {
        @Index(name = "idx_series_hero_block_series", columnList = "series_id")
})
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString
public class SeriesHeroBlock extends ContentBlock {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_series_hero_block_series"))
    @ToString.Exclude
    private Series series;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private SeriesHeroBlockType blockType;

    private Double x;

    private Double y;

    private Double width;

    private Double height;

    // IMAGE, LOGO
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // yalnızca IMAGE — arka plan görseline uygulanan blur şiddeti (px).
    // BUTTON'ın glassmorfik blur'u FE'de sabit CSS değeri (20px), bu
    // alanla karışmaz.
    @Column(name = "blur_amount")
    private Integer blurAmount;

    // yalnızca BOX
    @Column(name = "text_tr", columnDefinition = "TEXT")
    private String textTr;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    // yalnızca BOX — hex renk (ör. #1a1a2e)
    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    // IMAGE, BUTTON, BOX
    @Enumerated(EnumType.STRING)
    @Column(name = "border_radius", length = 10)
    private RadiusToken borderRadius;

    // TITLE, SYNOPSIS, META, BOX
    @Enumerated(EnumType.STRING)
    @Column(name = "font_family", length = 20)
    private HeroFontFamily fontFamily;

    @Column(name = "font_scale")
    @Builder.Default
    private Double fontScale = 1.0;
}
