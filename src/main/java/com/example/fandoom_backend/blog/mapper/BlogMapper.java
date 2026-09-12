package com.example.fandoom_backend.blog.mapper;

import com.example.fandoom_backend.blog.dto.BlogBlockResponse;
import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogTagResponse;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogBlock;
import com.example.fandoom_backend.blog.entity.BlogTag;
import com.example.fandoom_backend.common.util.LocalizedTextResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = LocalizedTextResolver.class)
public interface BlogMapper {

    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(blog.getTitleTr(), blog.getTitle()))")
    @Mapping(target = "imageAlt", expression = "java(LocalizedTextResolver.resolve(blog.getImageAltTr(), blog.getImageAlt()))")
    BlogSummaryResponse toSummaryResponse(Blog blog);

    List<BlogSummaryResponse> toSummaryResponseList(List<Blog> blogs);

    @Mapping(target = "content", expression = "java(LocalizedTextResolver.resolve(block.getContentTr(), block.getContent()))")
    @Mapping(target = "imageAlt", expression = "java(LocalizedTextResolver.resolve(block.getImageAltTr(), block.getImageAlt()))")
    BlogBlockResponse toBlockResponse(BlogBlock block);

    BlogTagResponse toTagResponse(BlogTag tag);

    // relatedBlogs entity'nin kendi alanı değil, servis katmanında (kürasyon+
    // algoritmik merdiven birleşimi) hesaplanıp ikinci parametreyle geçirilir.
    @Mapping(target = "relatedBlogs", source = "related")
    @Mapping(target = "title", expression = "java(LocalizedTextResolver.resolve(blog.getTitleTr(), blog.getTitle()))")
    @Mapping(target = "kicker", expression = "java(LocalizedTextResolver.resolve(blog.getKickerTr(), blog.getKicker()))")
    @Mapping(target = "axis", expression = "java(LocalizedTextResolver.resolve(blog.getAxisTr(), blog.getAxis()))")
    @Mapping(target = "imageAlt", expression = "java(LocalizedTextResolver.resolve(blog.getImageAltTr(), blog.getImageAlt()))")
    BlogDetailResponse toDetailResponse(Blog blog, List<BlogSummaryResponse> related);
}
