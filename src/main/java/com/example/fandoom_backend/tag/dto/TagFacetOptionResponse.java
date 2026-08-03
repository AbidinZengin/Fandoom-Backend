package com.example.fandoom_backend.tag.dto;

// Blog hub facet paneli için: bir tag'in kendisi + o tag'e sahip
// (belirli bir taggableType için) kaç kayıt olduğu. TagResponse'dan
// bilinçli olarak ayrı — description/type gibi facet panelinin
// ihtiyaç duymadığı alanları taşımaz, sadece sayaç göstermek için
// gereken minimum alanları içerir.
public record TagFacetOptionResponse(Long id, String name, String slug, long count) {}
