package com.example.fandoom_backend.cms.service;

import com.example.fandoom_backend.cms.dto.HomeBlockRequest;
import com.example.fandoom_backend.cms.dto.HomeBlockResponse;
import com.example.fandoom_backend.cms.entity.HomeBlock;
import com.example.fandoom_backend.cms.entity.PageName;
import com.example.fandoom_backend.cms.mapper.HomeBlockMapper;
import com.example.fandoom_backend.cms.repository.HomeBlockRepository;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeBlockServiceImpl implements HomeBlockService {

    private final HomeBlockRepository homeBlockRepository;
    private final HomeBlockMapper homeBlockMapper;

    @Override
    public List<HomeBlockResponse> getByPage(PageName page, Long entityId) {
        return homeBlockMapper.toResponseList(
                homeBlockRepository.findByPageAndEntityIdAndActiveTrueOrderByOrderIndexAsc(page, entityId));
    }

    @Override
    @Transactional
    public HomeBlockResponse create(HomeBlockRequest request) {
        HomeBlock homeBlock = HomeBlock.builder()
                .page(request.page())
                .entityId(request.entityId())
                .section(request.section())
                .contentType(request.contentType())
                .contentValue(request.contentValue())
                .linkUrl(request.linkUrl())
                .altText(request.altText())
                .active(request.active())
                .build();
        homeBlock.setOrderIndex(request.orderIndex());
        homeBlock.setCol(request.col());
        homeBlock.setRow(request.row());
        return homeBlockMapper.toResponse(homeBlockRepository.save(homeBlock));
    }

    @Override
    @Transactional
    public HomeBlockResponse update(Long id, HomeBlockRequest request) {
        HomeBlock homeBlock = findEntityById(id);
        homeBlock.setPage(request.page());
        homeBlock.setEntityId(request.entityId());
        homeBlock.setSection(request.section());
        homeBlock.setContentType(request.contentType());
        homeBlock.setContentValue(request.contentValue());
        homeBlock.setLinkUrl(request.linkUrl());
        homeBlock.setAltText(request.altText());
        homeBlock.setActive(request.active());
        homeBlock.setOrderIndex(request.orderIndex());
        homeBlock.setCol(request.col());
        homeBlock.setRow(request.row());
        return homeBlockMapper.toResponse(homeBlock);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!homeBlockRepository.existsById(id)) {
            throw new ResourceNotFoundException("İçerik bulunamadı: id=" + id);
        }
        homeBlockRepository.deleteById(id);
    }

    private HomeBlock findEntityById(Long id) {
        return homeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İçerik bulunamadı: id=" + id));
    }
}
