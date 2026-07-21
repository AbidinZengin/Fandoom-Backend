package com.example.fandoom_backend.series.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.franchise.service.FranchiseService;
import com.example.fandoom_backend.genre.service.GenreService;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.dto.SeriesRequest;
import com.example.fandoom_backend.series.dto.SeriesSummaryResponse;
import com.example.fandoom_backend.series.entity.Series;
import com.example.fandoom_backend.series.mapper.SeriesMapper;
import com.example.fandoom_backend.series.repository.SeriesRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeriesServiceImpl implements SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeriesMapper seriesMapper;
    private final FranchiseService franchiseService;
    private final GenreService genreService;
    private final ImageStorageService imageStorageService;

    @Override
    public PageResponse<SeriesSummaryResponse> list(Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findAll(pageable).map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<SeriesSummaryResponse> listByFranchise(Long franchiseId, Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findByFranchiseId(franchiseId, pageable)
                .map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<SeriesSummaryResponse> search(String query, Pageable pageable) {
        Page<SeriesSummaryResponse> page = seriesRepository.findByTitleContainingIgnoreCase(query, pageable)
                .map(seriesMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public SeriesDetailResponse getById(Long id) {
        return seriesMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    public SeriesDetailResponse getBySlug(String slug) {
        Series series = seriesRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: slug=" + slug));
        return seriesMapper.toDetailResponse(series);
    }

    @Override
    @Transactional
    public SeriesDetailResponse create(SeriesRequest request) {
        validateReferences(request.franchiseId(), request.genreIds());
        Series series = Series.builder()
                .title(request.title())
                .slug(SlugGenerator.generateUnique(request.title(), seriesRepository::existsBySlug))
                .synopsis(request.synopsis())
                .firstAirDate(request.firstAirDate())
                .status(request.status())
                .posterUrl(request.posterUrl())
                .coverImageUrl(request.coverImageUrl())
                .franchiseId(request.franchiseId())
                .genreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()))
                .build();
        return seriesMapper.toDetailResponse(seriesRepository.save(series));
    }

    @Override
    @Transactional
    public SeriesDetailResponse update(Long id, SeriesRequest request) {
        validateReferences(request.franchiseId(), request.genreIds());
        Series series = findEntityById(id);
        imageStorageService.deleteIfChanged(series.getPosterUrl(), request.posterUrl());
        imageStorageService.deleteIfChanged(series.getCoverImageUrl(), request.coverImageUrl());
        if (!series.getTitle().equals(request.title())) {
            series.setSlug(SlugGenerator.generateUnique(request.title(), seriesRepository::existsBySlug));
        }
        series.setTitle(request.title());
        series.setSynopsis(request.synopsis());
        series.setFirstAirDate(request.firstAirDate());
        series.setStatus(request.status());
        series.setPosterUrl(request.posterUrl());
        series.setCoverImageUrl(request.coverImageUrl());
        series.setFranchiseId(request.franchiseId());
        series.setGenreIds(request.genreIds() == null ? new HashSet<>() : new HashSet<>(request.genreIds()));
        return seriesMapper.toDetailResponse(series);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Series series = findEntityById(id);
        imageStorageService.delete(series.getPosterUrl());
        imageStorageService.delete(series.getCoverImageUrl());
        seriesRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return seriesRepository.existsById(id);
    }

    private void validateReferences(Long franchiseId, Set<Long> genreIds) {
        if (franchiseId != null && !franchiseService.existsById(franchiseId)) {
            throw new InvalidReferenceException("Geçersiz franchise id: " + franchiseId);
        }
        genreService.assertAllExist(genreIds);
    }

    private Series findEntityById(Long id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Series bulunamadı: id=" + id));
    }
}
