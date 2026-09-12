package com.example.fandoom_backend.community.entity;

// DELETED: soft-delete — CLAUDE.md'nin proje geneli "delete = hard delete"
// kuralına Community için bilinçli, dar kapsamlı bir istisna. Gerekçe:
// Comment.parent zinciri kırılmasın (bir yoruma verilen yanıtlar anlamsız
// kalmasın) ve moderasyon izni DB'de tutulabilsin diye. HIDDEN şimdilik
// yer tutucu — Faz 1'de bunu set eden bir endpoint yok.
public enum ThreadStatus {
    PUBLISHED, DELETED, HIDDEN
}
