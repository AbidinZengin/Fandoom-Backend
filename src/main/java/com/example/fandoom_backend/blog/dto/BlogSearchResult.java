package com.example.fandoom_backend.blog.dto;

// Quick search projeksiyonu: başlık ve kicker (alt satır) iki dilde ham taşınır, hangisinin gösterileceğine search/ karar verir.
public record BlogSearchResult(
        Long id, String slug, String title, String titleTr, String kicker, String kickerTr, String imageUrl) {}
