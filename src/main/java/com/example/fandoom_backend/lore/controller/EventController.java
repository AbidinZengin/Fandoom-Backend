package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.EventRequest;
import com.example.fandoom_backend.lore.dto.EventResponse;
import com.example.fandoom_backend.lore.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/api/lore/events/{id}")
    public EventResponse getById(@PathVariable Long id) {
        return eventService.getById(id);
    }

    @PostMapping("/api/lore/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/api/lore/events/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/api/lore/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/lore/events")
    public List<EventResponse> listForMovie(@PathVariable Long movieId) {
        return eventService.listForMovie(movieId);
    }

    @PostMapping("/api/movies/{movieId}/lore/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody EventRequest request) {
        return eventService.addToMovie(movieId, request);
    }

    @GetMapping("/api/series/{seriesId}/lore/events")
    public List<EventResponse> listForSeries(@PathVariable Long seriesId) {
        return eventService.listForSeries(seriesId);
    }

    @PostMapping("/api/series/{seriesId}/lore/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody EventRequest request) {
        return eventService.addToSeries(seriesId, request);
    }
}
