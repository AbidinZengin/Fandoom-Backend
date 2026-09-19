package com.example.fandoom_backend.community.mapper;

import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.ThreadDetailResponse;
import com.example.fandoom_backend.community.dto.ThreadMediaResponse;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.Thread;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// tags entity'nin kendi alanı değil (ayrı ThreadTag tablosu), author/isLiked/
// isBookmarked de entity'nin alanı değil (sırasıyla user/ modülünden ve
// ThreadLike/ThreadBookmark'tan) — servis katmanında ayrıca çözülüp ekstra
// parametrelerle geçirilir (BlogMapper.toDetailResponse(blog, related) deseniyle
// tutarlı).
@Mapper(componentModel = "spring")
public interface ThreadMapper {

    @Mapping(target = "id", source = "thread.id")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "isLiked", source = "liked")
    @Mapping(target = "isBookmarked", source = "bookmarked")
    @Mapping(target = "media", source = "media")
    @Mapping(target = "excerpt", expression = "java(buildExcerpt(thread.getBody()))")
    ThreadSummaryResponse toSummaryResponse(
            Thread thread, List<String> tags, AuthorSummary author, boolean liked, boolean bookmarked,
            List<ThreadMediaResponse> media);

    @Mapping(target = "id", source = "thread.id")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "isLiked", source = "liked")
    @Mapping(target = "isBookmarked", source = "bookmarked")
    @Mapping(target = "media", source = "media")
    ThreadDetailResponse toDetailResponse(
            Thread thread, List<String> tags, AuthorSummary author, boolean liked, boolean bookmarked,
            List<ThreadMediaResponse> media);

    // 160 karakter sınırı, kelime ortasından kesmez (son boşluğa geri sarar).
    default String buildExcerpt(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String trimmed = body.trim();
        int maxLength = 160;
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        String cut = trimmed.substring(0, maxLength);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > 0) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "...";
    }
}
