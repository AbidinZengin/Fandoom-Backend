package com.example.fandoom_backend.group.controller;

import com.example.fandoom_backend.group.dto.GroupRequest;
import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.GroupType;
import com.example.fandoom_backend.group.service.GroupService;
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

    @GetMapping("/api/groups/{id}")
    public GroupResponse getById(@PathVariable Long id) {
        return groupService.getById(id);
    }

    @GetMapping("/api/groups/slug/{slug}")
    public GroupResponse getBySlug(@PathVariable String slug) {
        return groupService.getBySlug(slug);
    }

    @PutMapping("/api/groups/{id}")
    public GroupResponse update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return groupService.update(id, request);
    }

    @DeleteMapping("/api/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groupService.delete(id);
    }

    @GetMapping("/api/movies/{movieId}/groups")
    public List<GroupResponse> listForMovie(@PathVariable Long movieId,
                                             @RequestParam(required = false) GroupType type) {
        return groupService.listForMovie(movieId, type);
    }

    @PostMapping("/api/movies/{movieId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse addToMovie(@PathVariable Long movieId, @Valid @RequestBody GroupRequest request) {
        return groupService.addToMovie(movieId, request);
    }

    @GetMapping("/api/series/{seriesId}/groups")
    public List<GroupResponse> listForSeries(@PathVariable Long seriesId,
                                              @RequestParam(required = false) GroupType type) {
        return groupService.listForSeries(seriesId, type);
    }

    @PostMapping("/api/series/{seriesId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse addToSeries(@PathVariable Long seriesId, @Valid @RequestBody GroupRequest request) {
        return groupService.addToSeries(seriesId, request);
    }
}
