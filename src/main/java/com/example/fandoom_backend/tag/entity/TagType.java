package com.example.fandoom_backend.tag.entity;

// Blog hub facet filtreleri için kullanılan tag alt-kategorileri. Mevcut
// Movie/Series/Person/Character tag'leri bu alanı hiç doldurmaz (type=null),
// geriye dönük uyumluluk bu şekilde korunur.
public enum TagType {
    FORMAT, MOOD, THEME
}
