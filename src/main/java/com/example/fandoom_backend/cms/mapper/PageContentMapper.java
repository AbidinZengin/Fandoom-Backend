package com.example.fandoom_backend.cms.mapper;

import com.example.fandoom_backend.cms.dto.PageContentResponse;
import com.example.fandoom_backend.cms.entity.PageContent;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PageContentMapper {
    PageContentResponse toResponse(PageContent pageContent);
    List<PageContentResponse> toResponseList(List<PageContent> pageContents);
}
