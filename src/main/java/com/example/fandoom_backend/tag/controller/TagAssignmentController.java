package com.example.fandoom_backend.tag.controller;

import com.example.fandoom_backend.tag.dto.TagAssignmentRequest;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.entity.TaggableType;
import com.example.fandoom_backend.tag.service.TagAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagAssignmentController {

    private final TagAssignmentService tagAssignmentService;

    @GetMapping("/assignments")
    public List<TagAssignmentResponse> listForTarget(
            @RequestParam TaggableType taggableType,
            @RequestParam Long taggableId) {
        return tagAssignmentService.listForTarget(taggableType, taggableId);
    }

    @PostMapping("/{tagId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public TagAssignmentResponse assign(
            @PathVariable Long tagId,
            @Valid @RequestBody TagAssignmentRequest request) {
        return tagAssignmentService.assign(tagId, request);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagAssignmentService.delete(id);
    }
}
