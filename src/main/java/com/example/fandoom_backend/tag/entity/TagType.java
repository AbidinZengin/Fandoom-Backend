package com.example.fandoom_backend.tag.entity;

// Tag alt-kategorisi (type=null: sıradan serbest tag). Mevcut Movie/Series/
// Person/Character tag'leri ve bloglara serbestçe eklenen konu etiketleri
// bu alanı hiç doldurmaz, geriye dönük uyumluluk bu şekilde korunur.
// MOOD: Blog hub facet filtresi. CONTENT_WARNING: içerik uyarısı (ör. şiddet,
// flaş ışık) — herhangi bir TaggableType'a (Movie/Series/...) atanabilir,
// TagAssignment üzerinden okunur, ayrı bir alan/tablo yok. FORMAT artık burada
// değil (Blog.format enum'u), THEME de kaldırıldı (type=null tag'ler üstlendi).
public enum TagType {
    MOOD,
    CONTENT_WARNING
}
