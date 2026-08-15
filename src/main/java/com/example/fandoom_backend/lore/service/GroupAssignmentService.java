package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.GroupAssignmentRequest;
import com.example.fandoom_backend.lore.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.lore.entity.TaggableType;

import java.util.List;

public interface GroupAssignmentService {
    List<GroupAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId);
    GroupAssignmentResponse assign(Long groupId, GroupAssignmentRequest request);
    void delete(Long id);
}
