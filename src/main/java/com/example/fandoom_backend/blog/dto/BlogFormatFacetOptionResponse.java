package com.example.fandoom_backend.blog.dto;

import com.example.fandoom_backend.blog.entity.BlogFormat;

// Blog hub facet paneli için: bir format değeri + o formata sahip
// (yayınlanmış) blog sayısı. FranchiseFacetOptionResponse/TagFacetOptionResponse
// ile aynı desen, ama format artık tag değil Blog'un kendi enum alanı
// olduğu için ayrı bir DTO.
public record BlogFormatFacetOptionResponse(BlogFormat format, long count) {}
