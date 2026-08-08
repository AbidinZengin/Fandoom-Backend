package com.example.fandoom_backend.group.service;

import com.example.fandoom_backend.group.dto.GroupAssignmentRequest;
import com.example.fandoom_backend.group.dto.GroupAssignmentResponse;
import com.example.fandoom_backend.group.entity.TaggableType;

import java.util.List;

public interface GroupAssignmentService {
    List<GroupAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId);
    GroupAssignmentResponse assign(Long groupId, GroupAssignmentRequest request);
    void delete(Long id);
}
