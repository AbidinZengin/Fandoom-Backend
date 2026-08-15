package com.example.fandoom_backend.lore.controller;

import com.example.fandoom_backend.lore.dto.GroupAssignmentRequest;
import com.example.fandoom_backend.lore.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.lore.entity.TaggableType;
import com.example.fandoom_backend.lore.service.GroupAssignmentService;
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
@RequestMapping("/api/lore/groups")
@RequiredArgsConstructor
public class GroupAssignmentController {

    private final GroupAssignmentService groupAssignmentService;

    @GetMapping("/assignments")
    public List<GroupAssignmentResponse> listForTarget(
            @RequestParam TaggableType taggableType,
            @RequestParam Long taggableId) {
        return groupAssignmentService.listForTarget(taggableType, taggableId);
    }

    @PostMapping("/{groupId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupAssignmentResponse assign(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupAssignmentRequest request) {
        return groupAssignmentService.assign(groupId, request);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groupAssignmentService.delete(id);
    }
}
