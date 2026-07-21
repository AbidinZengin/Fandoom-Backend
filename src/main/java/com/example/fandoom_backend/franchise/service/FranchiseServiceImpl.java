package com.example.fandoom_backend.franchise.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.franchise.dto.FranchiseDetailResponse;
import com.example.fandoom_backend.franchise.dto.FranchiseRequest;
import com.example.fandoom_backend.franchise.dto.FranchiseSummaryResponse;
import com.example.fandoom_backend.franchise.entity.Franchise;
import com.example.fandoom_backend.franchise.mapper.FranchiseMapper;
import com.example.fandoom_backend.franchise.repository.FranchiseRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FranchiseServiceImpl implements FranchiseService {

    private final FranchiseRepository franchiseRepository;
    private final FranchiseMapper franchiseMapper;
    private final ImageStorageService imageStorageService;

    @Override
    public PageResponse<FranchiseSummaryResponse> list(Pageable pageable) {
        Page<FranchiseSummaryResponse> page = franchiseRepository.findAll(pageable)
                .map(franchiseMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public FranchiseDetailResponse getById(Long id) {
        return franchiseMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    public FranchiseDetailResponse getBySlug(String slug) {
        Franchise franchise = franchiseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise bulunamadı: slug=" + slug));
        return franchiseMapper.toDetailResponse(franchise);
    }

    @Override
    @Transactional
    public FranchiseDetailResponse create(FranchiseRequest request) {
        Franchise franchise = Franchise.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), franchiseRepository::existsBySlug))
                .description(request.description())
                .logoUrl(request.logoUrl())
                .bannerImageUrl(request.bannerImageUrl())
                .build();
        return franchiseMapper.toDetailResponse(franchiseRepository.save(franchise));
    }

    @Override
    @Transactional
    public FranchiseDetailResponse update(Long id, FranchiseRequest request) {
        Franchise franchise = findEntityById(id);
        imageStorageService.deleteIfChanged(franchise.getLogoUrl(), request.logoUrl());
        imageStorageService.deleteIfChanged(franchise.getBannerImageUrl(), request.bannerImageUrl());
        if (!franchise.getName().equals(request.name())) {
            franchise.setSlug(SlugGenerator.generateUnique(request.name(), franchiseRepository::existsBySlug));
        }
        franchise.setName(request.name());
        franchise.setDescription(request.description());
        franchise.setLogoUrl(request.logoUrl());
        franchise.setBannerImageUrl(request.bannerImageUrl());
        return franchiseMapper.toDetailResponse(franchise);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Franchise franchise = findEntityById(id);
        imageStorageService.delete(franchise.getLogoUrl());
        imageStorageService.delete(franchise.getBannerImageUrl());
        franchiseRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return franchiseRepository.existsById(id);
    }

    private Franchise findEntityById(Long id) {
        return franchiseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise bulunamadı: id=" + id));
    }
}
