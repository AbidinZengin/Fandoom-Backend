package com.example.fandoom_backend.search.dto;

// Header arama kutusundaki tek satırın ortak şekli: başlık + gri alt satır + küçük görsel.
// title/subtitle istek diline göre çözülmüş gelir.
public record SearchHit(Long id, String slug, String title, String subtitle, String imageUrl) {}
