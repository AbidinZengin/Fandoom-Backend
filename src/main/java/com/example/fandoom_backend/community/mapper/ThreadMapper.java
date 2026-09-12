package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.Thread;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// tags entity'nin kendi alanı değil (ayrı ThreadTag tablosu) — servis
// katmanında ayrıca sorgulanıp ikinci parametreyle geçirilir (BlogMapper.
// toDetailResponse(blog, related) deseniyle tutarlı).
@Mapper(componentModel = "spring")
public interface ThreadMapper {

    @Mapping(target = "tags", source = "tags")
    ThreadSummaryResponse toSummaryResponse(Thread thread, List<String> tags);

    @Mapping(target = "tags", source = "tags")
    ThreadDetailResponse toDetailResponse(Thread thread, List<String> tags);
}
