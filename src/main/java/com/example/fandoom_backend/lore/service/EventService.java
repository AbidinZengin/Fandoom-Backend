package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.EventRequest;
import com.example.fandoom_backend.lore.dto.EventResponse;

import java.util.List;

public interface EventService {
    List<EventResponse> listForMovie(Long movieId);
    List<EventResponse> listForSeries(Long seriesId);
    EventResponse getById(Long id);
    EventResponse addToMovie(Long movieId, EventRequest request);
    EventResponse addToSeries(Long seriesId, EventRequest request);
    EventResponse update(Long id, EventRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
