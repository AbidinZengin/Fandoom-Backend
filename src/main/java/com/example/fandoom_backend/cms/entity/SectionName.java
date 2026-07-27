package com.example.fandoom_backend.cms.entity;

public enum SectionName {

    // Genel / site geneli (GLOBAL sayfası)
    LOGO,
    ANNOUNCEMENT_BAR,
    FOOTER_TEXT,

    // Anasayfa (HOME)
    HERO_BANNER,
    HERO_SUBTITLE,
    PROMO_TEXT,
    FEATURED_SECTION_TITLE,

    // Liste sayfaları (FRANCHISE_LIST vb.)
    INTRO_TEXT,

    // Detay sayfaları (SERIES_DETAIL vb.)
    SYNOPSIS_HIGHLIGHT,
    CAST_HIGHLIGHT,
    BEHIND_THE_SCENES,
    FAN_QUOTE,
    TRAILER_EMBED,
    GALLERY,

    // Bileşik kart listeleri (ScrollStepper, TabExhibit, Intro beats/stats):
    // bir kartın alanları (görsel/başlık/açıklama) AYRI kayıtlardır, aynı
    // orderIndex'i paylaşarak gruplanır — frontend'de section'a göre alan
    // adına eşlenir, orderIndex'e göre kartlara toplanır (bkz. CLAUDE.md).
    STEPPER_ITEM_IMAGE,
    STEPPER_ITEM_TITLE,
    STEPPER_ITEM_DESCRIPTION,
    EXHIBIT_ITEM_IMAGE,
    EXHIBIT_ITEM_LABEL,
    EXHIBIT_TITLE,
    INTRO_EYEBROW,
    INTRO_HEADLINE,
    INTRO_CLOSING,
    INTRO_STAT_VALUE,
    INTRO_STAT_LABEL,
    INTRO_BEAT_IMAGE,
    INTRO_BEAT_TITLE,
    INTRO_BEAT_BODY
}
