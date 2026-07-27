package com.example.fandoom_backend.cms.service;

import com.example.fandoom_backend.cms.dto.PageContentRequest;
import com.example.fandoom_backend.cms.dto.PageContentResponse;
import com.example.fandoom_backend.cms.entity.PageName;

import java.util.List;

public interface PageContentService {
    List<PageContentResponse> getByPage(PageName page, Long entityId);
    PageContentResponse create(PageContentRequest request);
    PageContentResponse update(Long id, PageContentRequest request);
    void delete(Long id);
}
