package com.example.fandoom_backend.cms.service;

import com.example.fandoom_backend.cms.dto.PageContentRequest;
import com.example.fandoom_backend.cms.dto.PageContentResponse;
import com.example.fandoom_backend.cms.entity.PageContent;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.mapper.PageContentMapper;
import com.example.fandoom_backend.cms.repository.PageContentRepository;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageContentServiceImpl implements PageContentService {

    private final PageContentRepository pageContentRepository;
    private final PageContentMapper pageContentMapper;

    @Override
    public List<PageContentResponse> getByPage(PageName page, Long entityId) {
        return pageContentMapper.toResponseList(
                pageContentRepository.findByPageAndEntityIdAndActiveTrueOrderByOrderIndexAsc(page, entityId));
    }

    @Override
    @Transactional
    public PageContentResponse create(PageContentRequest request) {
        PageContent pageContent = PageContent.builder()
                .page(request.page())
                .entityId(request.entityId())
                .section(request.section())
                .contentType(request.contentType())
                .contentValue(request.contentValue())
                .linkUrl(request.linkUrl())
                .altText(request.altText())
                .active(request.active())
                .orderIndex(request.orderIndex())
                .build();
        return pageContentMapper.toResponse(pageContentRepository.save(pageContent));
    }

    @Override
    @Transactional
    public PageContentResponse update(Long id, PageContentRequest request) {
        PageContent pageContent = findEntityById(id);
        pageContent.setPage(request.page());
        pageContent.setEntityId(request.entityId());
        pageContent.setSection(request.section());
        pageContent.setContentType(request.contentType());
        pageContent.setContentValue(request.contentValue());
        pageContent.setLinkUrl(request.linkUrl());
        pageContent.setAltText(request.altText());
        pageContent.setActive(request.active());
        pageContent.setOrderIndex(request.orderIndex());
        return pageContentMapper.toResponse(pageContent);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!pageContentRepository.existsById(id)) {
            throw new ResourceNotFoundException("İçerik bulunamadı: id=" + id);
        }
        pageContentRepository.deleteById(id);
    }

    private PageContent findEntityById(Long id) {
        return pageContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İçerik bulunamadı: id=" + id));
    }
}
