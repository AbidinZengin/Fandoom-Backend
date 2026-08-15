package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.lore.dto.EventRequest;
import com.example.fandoom_backend.lore.dto.EventResponse;
import com.example.fandoom_backend.lore.entity.Event;
import com.example.fandoom_backend.lore.entity.Location;
import com.example.fandoom_backend.lore.entity.SubjectType;
import com.example.fandoom_backend.lore.mapper.EventMapper;
import com.example.fandoom_backend.lore.repository.EventRepository;
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
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final ImageStorageService imageStorageService;
    private final MovieService movieService;
    private final SeriesService seriesService;

    @Override
    public List<EventResponse> listForMovie(Long movieId) {
        return eventMapper.toResponseList(
                eventRepository.findBySubjectTypeAndSubjectIdOrderByOrderIndexAsc(SubjectType.MOVIE, movieId));
    }

    @Override
    public List<EventResponse> listForSeries(Long seriesId) {
        return eventMapper.toResponseList(
                eventRepository.findBySubjectTypeAndSubjectIdOrderByOrderIndexAsc(SubjectType.SERIES, seriesId));
    }

    @Override
    public EventResponse getById(Long id) {
        return eventMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public EventResponse addToMovie(Long movieId, EventRequest request) {
        if (!movieService.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie bulunamadı: id=" + movieId);
        }
        Location location = resolveLocation(request.locationId(), SubjectType.MOVIE, movieId);
        return eventMapper.toResponse(eventRepository.save(buildEvent(SubjectType.MOVIE, movieId, location, request)));
    }

    @Override
    @Transactional
    public EventResponse addToSeries(Long seriesId, EventRequest request) {
        if (!seriesService.existsById(seriesId)) {
            throw new ResourceNotFoundException("Series bulunamadı: id=" + seriesId);
        }
        Location location = resolveLocation(request.locationId(), SubjectType.SERIES, seriesId);
        return eventMapper.toResponse(eventRepository.save(buildEvent(SubjectType.SERIES, seriesId, location, request)));
    }

    @Override
    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        Event event = findEntityById(id);
        Location location = resolveLocation(request.locationId(), event.getSubjectType(), event.getSubjectId());
        imageStorageService.deleteIfChanged(event.getImageUrl(), request.imageUrl());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setOrderIndex(request.orderIndex());
        event.setImageUrl(request.imageUrl());
        event.setLocation(location);
        event.setPinned(request.pinned());
        event.setCustomFields(request.customFields());
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Event event = findEntityById(id);
        imageStorageService.delete(event.getImageUrl());
        eventRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return eventRepository.existsById(id);
    }

    // Location verilmişse, Event ile aynı yapıma ait olmalı — başka bir
    // yapımın lokasyonu yanlışlıkla bağlanmasın diye.
    private Location resolveLocation(Long locationId, SubjectType expectedSubjectType, Long expectedSubjectId) {
        if (locationId == null) {
            return null;
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz location id: " + locationId));
        if (location.getSubjectType() != expectedSubjectType || !location.getSubjectId().equals(expectedSubjectId)) {
            throw new InvalidReferenceException("Location bu yapıma ait değil: locationId=" + locationId);
        }
        return location;
    }

    private Event buildEvent(SubjectType subjectType, Long subjectId, Location location, EventRequest request) {
        return Event.builder()
                .name(request.name())
                .description(request.description())
                .orderIndex(request.orderIndex())
                .imageUrl(request.imageUrl())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .location(location)
                .pinned(request.pinned())
                .customFields(request.customFields())
                .build();
    }

    private Event findEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event bulunamadı: id=" + id));
    }
}
