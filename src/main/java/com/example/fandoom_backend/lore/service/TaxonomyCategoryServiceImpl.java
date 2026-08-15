package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryRequest;
import com.example.fandoom_backend.lore.dto.TaxonomyCategoryResponse;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import com.example.fandoom_backend.lore.mapper.TaxonomyCategoryMapper;
import com.example.fandoom_backend.lore.repository.TaxonomyCategoryRepository;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonomyCategoryServiceImpl implements TaxonomyCategoryService {

    private final TaxonomyCategoryRepository taxonomyCategoryRepository;
    private final TaxonomyCategoryMapper taxonomyCategoryMapper;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<TaxonomyCategoryResponse> listForMovie(Long movieId) {
        return taxonomyCategoryMapper.toResponseList(
                taxonomyCategoryRepository.findBySubjectTypeAndSubjectId(SubjectType.MOVIE, movieId));
    }

    @Override
    public List<TaxonomyCategoryResponse> listForSeries(Long seriesId) {
        return taxonomyCategoryMapper.toResponseList(
                taxonomyCategoryRepository.findBySubjectTypeAndSubjectId(SubjectType.SERIES, seriesId));
    }

    @Override
    public TaxonomyCategoryResponse getById(Long id) {
        return taxonomyCategoryMapper.toResponse(findEntityById(id));
    }

    @Override
    public TaxonomyCategoryResponse getBySlug(String slug) {
        TaxonomyCategory category = taxonomyCategoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: slug=" + slug));
        return taxonomyCategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public TaxonomyCategoryResponse create(TaxonomyCategoryRequest request) {
        if (request.subjectType() == null || request.subjectId() == null) {
            throw new InvalidReferenceException("subjectType ve subjectId zorunlu");
        }
        return switch (request.subjectType()) {
            case MOVIE -> addToMovie(request.subjectId(), request);
            case SERIES -> addToSeries(request.subjectId(), request);
        };
    }

    @Override
    @Transactional
    public TaxonomyCategoryResponse addToMovie(Long movieId, TaxonomyCategoryRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        return taxonomyCategoryMapper.toResponse(
                taxonomyCategoryRepository.save(buildCategory(SubjectType.MOVIE, movieId, request)));
    }

    @Override
    @Transactional
    public TaxonomyCategoryResponse addToSeries(Long seriesId, TaxonomyCategoryRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        return taxonomyCategoryMapper.toResponse(
                taxonomyCategoryRepository.save(buildCategory(SubjectType.SERIES, seriesId, request)));
    }

    @Override
    @Transactional
    public TaxonomyCategoryResponse update(Long id, TaxonomyCategoryRequest request) {
        TaxonomyCategory category = findEntityById(id);
        if (!category.getName().equals(request.name())) {
            category.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> taxonomyCategoryRepository.existsBySlugAndIdNot(slug, id)));
        }
        category.setName(request.name());
        return taxonomyCategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!taxonomyCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kategori bulunamadı: id=" + id);
        }
        taxonomyCategoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return taxonomyCategoryRepository.existsById(id);
    }

    private TaxonomyCategory buildCategory(SubjectType subjectType, Long subjectId, TaxonomyCategoryRequest request) {
        return TaxonomyCategory.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), taxonomyCategoryRepository::existsBySlug))
                .subjectType(subjectType)
                .subjectId(subjectId)
                .build();
    }

    private TaxonomyCategory findEntityById(Long id) {
        return taxonomyCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: id=" + id));
    }
}
