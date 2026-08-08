package com.example.fandoom_backend.tag.entity;

// Blog hub facet filtresi için kullanılan tag alt-kategorisi. Mevcut
// Movie/Series/Person/Character tag'leri (ve bloglara serbestçe eklenen
// konu etiketleri, ör. "Daenerys", "Character Analysis") bu alanı hiç
// doldurmaz (type=null), geriye dönük uyumluluk bu şekilde korunur.
// FORMAT artık burada değil — Blog.format alanına (sabit/kararlı liste
// olduğu için enum) taşındı. THEME de kaldırıldı — görevini sıradan
// (type=null) serbest tag'ler üstleniyor artık, ayrı bir facet tipi
// olmasına gerek kalmadı.
public enum TagType {
    MOOD
}
