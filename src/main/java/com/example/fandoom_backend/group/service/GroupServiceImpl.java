package com.example.fandoom_backend.group.service;

import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.group.dto.GroupRequest;
import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.Group;
import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.entity.SubjectType;
import com.example.fandoom_backend.group.mapper.GroupMapper;
import com.example.fandoom_backend.group.repository.GroupRepository;
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
    private final GroupMapper groupMapper;
    private final ImageStorageService imageStorageService;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<GroupResponse> listForMovie(Long movieId, GroupType type) {
        return groupMapper.toResponseList(type == null
                ? groupRepository.findBySubjectTypeAndSubjectId(SubjectType.MOVIE, movieId)
                : groupRepository.findBySubjectTypeAndSubjectIdAndType(SubjectType.MOVIE, movieId, type));
    }

    @Override
    public List<GroupResponse> listForSeries(Long seriesId, GroupType type) {
        return groupMapper.toResponseList(type == null
                ? groupRepository.findBySubjectTypeAndSubjectId(SubjectType.SERIES, seriesId)
                : groupRepository.findBySubjectTypeAndSubjectIdAndType(SubjectType.SERIES, seriesId, type));
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
    public GroupResponse addToMovie(Long movieId, GroupRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        return groupMapper.toResponse(groupRepository.save(buildGroup(SubjectType.MOVIE, movieId, request)));
    }

    @Override
    @Transactional
    public GroupResponse addToSeries(Long seriesId, GroupRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        return groupMapper.toResponse(groupRepository.save(buildGroup(SubjectType.SERIES, seriesId, request)));
    }

    @Override
    @Transactional
    public GroupResponse update(Long id, GroupRequest request) {
        Group group = findEntityById(id);
        imageStorageService.deleteIfChanged(group.getImageUrl(), request.imageUrl());
        if (!group.getName().equals(request.name())) {
            group.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> groupRepository.existsBySlugAndIdNot(slug, id)));
        }
        group.setName(request.name());
        group.setImageUrl(request.imageUrl());
        group.setType(request.type());
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

    private Group buildGroup(SubjectType subjectType, Long subjectId, GroupRequest request) {
        return Group.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), groupRepository::existsBySlug))
                .imageUrl(request.imageUrl())
                .type(request.type())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .build();
    }

    private Group findEntityById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group bulunamadı: id=" + id));
    }
}
