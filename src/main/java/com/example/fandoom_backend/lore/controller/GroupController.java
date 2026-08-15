package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.GroupRequest;
import com.example.fandoom_backend.lore.dto.GroupResponse;
import com.example.fandoom_backend.lore.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/api/lore/groups/{id}")
    public GroupResponse getById(@PathVariable Long id) {
        return groupService.getById(id);
    }

    @GetMapping("/api/lore/groups/slug/{slug}")
    public GroupResponse getBySlug(@PathVariable String slug) {
        return groupService.getBySlug(slug);
    }

    @PutMapping("/api/lore/groups/{id}")
    public GroupResponse update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return groupService.update(id, request);
    }

    @DeleteMapping("/api/lore/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groupService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/lore/groups")
    public List<GroupResponse> listForMovie(@PathVariable Long movieId,
                                             @RequestParam(required = false) Long categoryId) {
        return groupService.listForMovie(movieId, categoryId);
    }

    @PostMapping("/api/movies/{movieId}/lore/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody GroupRequest request) {
        return groupService.addToMovie(movieId, request);
    }

    @GetMapping("/api/series/{seriesId}/lore/groups")
    public List<GroupResponse> listForSeries(@PathVariable Long seriesId,
                                              @RequestParam(required = false) Long categoryId) {
        return groupService.listForSeries(seriesId, categoryId);
    }

    @PostMapping("/api/series/{seriesId}/lore/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody GroupRequest request) {
        return groupService.addToSeries(seriesId, request);
    }
}
