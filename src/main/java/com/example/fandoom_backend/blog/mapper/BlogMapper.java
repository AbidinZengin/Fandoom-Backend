package com.example.fandoom_backend.blog.mapper;

import com.example.fandoom_backend.blog.dto.BlogBlockResponse;
import com.example.fandoom_backend.blog.dto.BlogDetailResponse;
import com.example.fandoom_backend.blog.dto.BlogSummaryResponse;
import com.example.fandoom_backend.blog.dto.BlogTagResponse;
import com.example.fandoom_backend.blog.entity.Blog;
import com.example.fandoom_backend.blog.entity.BlogBlock;
import com.example.fandoom_backend.blog.entity.BlogTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    BlogSummaryResponse toSummaryResponse(Blog blog);

    List<BlogSummaryResponse> toSummaryResponseList(List<Blog> blogs);

    BlogBlockResponse toBlockResponse(BlogBlock block);

    BlogTagResponse toTagResponse(BlogTag tag);

    // relatedBlogs entity'nin kendi alanı değil, servis katmanında (kürasyon+
    // algoritmik merdiven birleşimi) hesaplanıp ikinci parametreyle geçirilir.
    @Mapping(target = "relatedBlogs", source = "related")
    BlogDetailResponse toDetailResponse(Blog blog, List<BlogSummaryResponse> related);
}
