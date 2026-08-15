package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.common.util.SlugGenerator;
import com.example.fandoom_backend.lore.dto.LocationRequest;
import com.example.fandoom_backend.lore.dto.LocationResponse;
import com.example.fandoom_backend.lore.entity.Location;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.mapper.LocationMapper;
import com.example.fandoom_backend.lore.repository.LocationRepository;
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
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final ImageStorageService imageStorageService;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<LocationResponse> listForMovie(Long movieId) {
        return locationMapper.toResponseList(
                locationRepository.findBySubjectTypeAndSubjectId(SubjectType.MOVIE, movieId));
    }

    @Override
    public List<LocationResponse> listForSeries(Long seriesId) {
        return locationMapper.toResponseList(
                locationRepository.findBySubjectTypeAndSubjectId(SubjectType.SERIES, seriesId));
    }

    @Override
    public LocationResponse getById(Long id) {
        return locationMapper.toResponse(findEntityById(id));
    }

    @Override
    public LocationResponse getBySlug(String slug) {
        Location location = locationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Location bulunamadı: slug=" + slug));
        return locationMapper.toResponse(location);
    }

    @Override
    @Transactional
    public LocationResponse create(LocationRequest request) {
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
    public LocationResponse addToMovie(Long movieId, LocationRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        return locationMapper.toResponse(locationRepository.save(buildLocation(SubjectType.MOVIE, movieId, request)));
    }

    @Override
    @Transactional
    public LocationResponse addToSeries(Long seriesId, LocationRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        return locationMapper.toResponse(locationRepository.save(buildLocation(SubjectType.SERIES, seriesId, request)));
    }

    @Override
    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = findEntityById(id);
        imageStorageService.deleteIfChanged(location.getImageUrl(), request.imageUrl());
        if (!location.getName().equals(request.name())) {
            location.setSlug(SlugGenerator.generateUnique(request.name(),
                    slug -> locationRepository.existsBySlugAndIdNot(slug, id)));
        }
        location.setName(request.name());
        location.setImageUrl(request.imageUrl());
        location.setDescription(request.description());
        location.setCustomFields(request.customFields());
        return locationMapper.toResponse(location);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Location location = findEntityById(id);
        imageStorageService.delete(location.getImageUrl());
        locationRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return locationRepository.existsById(id);
    }

    private Location buildLocation(SubjectType subjectType, Long subjectId, LocationRequest request) {
        return Location.builder()
                .name(request.name())
                .slug(SlugGenerator.generateUnique(request.name(), locationRepository::existsBySlug))
                .imageUrl(request.imageUrl())
                .description(request.description())
                .customFields(request.customFields())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .build();
    }

    private Location findEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location bulunamadı: id=" + id));
    }
}
