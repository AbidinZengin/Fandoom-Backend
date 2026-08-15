package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.lore.dto.GroupRequest;
import com.example.fandoom_backend.lore.dto.GroupResponse;
import com.example.fandoom_backend.lore.entity.Group;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.entity.TaxonomyCategory;
import com.example.fandoom_backend.lore.mapper.GroupMapper;
import com.example.fandoom_backend.lore.repository.GroupRepository;
import com.example.fandoom_backend.lore.repository.TaxonomyCategoryRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.movie.service.MovieService;
import com.example.fandoom_backend.series.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final TaxonomyCategoryRepository taxonomyCategoryRepository;
    private final GroupMapper groupMapper;
    private final ImageStorageService imageStorageService;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<GroupResponse> listForMovie(Long movieId, Long categoryId) {
        return groupMapper.toResponseList(categoryId == null
                ? groupRepository.findByCategory_SubjectTypeAndCategory_SubjectId(SubjectType.MOVIE, movieId)
                : groupRepository.findByCategory_SubjectTypeAndCategory_SubjectIdAndCategoryId(
                        SubjectType.MOVIE, movieId, categoryId));
    }

    @Override
    public List<GroupResponse> listForSeries(Long seriesId, Long categoryId) {
        return groupMapper.toResponseList(categoryId == null
                ? groupRepository.findByCategory_SubjectTypeAndCategory_SubjectId(SubjectType.SERIES, seriesId)
                : groupRepository.findByCategory_SubjectTypeAndCategory_SubjectIdAndCategoryId(
                        SubjectType.SERIES, seriesId, categoryId));
    }

    @Override
    public GroupResponse getById(Long id) {
        return groupMapper.toResponse(findEntityById(id));
    }

    @Override
    public GroupResponse getBySlug(String slug) {
        Group group = groupRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Group bulunamadı: slug=" + slug));
        return groupMapper.toResponse(group);
    }

    @Override
    @Transactional
    public GroupResponse create(GroupRequest request) {
        TaxonomyCategory category = taxonomyCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz category id: " + request.categoryId()));
        return groupMapper.toResponse(groupRepository.save(buildGroup(category, request)));
    }

    @Override
    @Transactional
    public GroupResponse addToMovie(Long movieId, GroupRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        TaxonomyCategory category = resolveCategory(request.categoryId(), SubjectType.MOVIE, movieId);
        return groupMapper.toResponse(groupRepository.save(buildGroup(category, request)));
    }

    @Override
    @Transactional
    public GroupResponse addToSeries(Long seriesId, GroupRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        TaxonomyCategory category = resolveCategory(request.categoryId(), SubjectType.SERIES, seriesId);
        return groupMapper.toResponse(groupRepository.save(buildGroup(category, request)));
    }

    @Override
    @Transactional
    public GroupResponse update(Long id, GroupRequest request) {
        Group group = findEntityById(id);
        TaxonomyCategory category = resolveCategory(request.categoryId(),
                group.getCategory().getSubjectType(), group.getCategory().getSubjectId());
        imageStorageService.deleteIfChanged(group.getImageUrl(), request.imageUrl());
        if (!group.getName().equals(request.name())) {
            group.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> groupRepository.existsBySlugAndIdNot(slug, id)));
        }
        group.setName(request.name());
        group.setImageUrl(request.imageUrl());
        group.setCategory(category);
        group.setCustomFields(request.customFields());
        return groupMapper.toResponse(group);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Group group = findEntityById(id);
        imageStorageService.delete(group.getImageUrl());
        groupRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return groupRepository.existsById(id);
    }

    // Group'un kategorisi her zaman kendi yapımına (subjectType/subjectId) ait
    // olmalı — başka bir yapımın kategorisi yanlışlıkla bağlanmasın diye.
    private TaxonomyCategory resolveCategory(Long categoryId, SubjectType expectedSubjectType, Long expectedSubjectId) {
        TaxonomyCategory category = taxonomyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz category id: " + categoryId));
        if (category.getSubjectType() != expectedSubjectType || !category.getSubjectId().equals(expectedSubjectId)) {
            throw new InvalidReferenceException("Kategori bu yapıma ait değil: categoryId=" + categoryId);
        }
        return category;
    }

    private Group buildGroup(TaxonomyCategory category, GroupRequest request) {
        return Group.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), groupRepository::existsBySlug))
                .imageUrl(request.imageUrl())
                .category(category)
                .customFields(request.customFields())
                .build();
    }

    private Group findEntityById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group bulunamadı: id=" + id));
    }
}
