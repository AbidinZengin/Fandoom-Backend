package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.tag.dto.TagAssignmentRequest;
import com.example.fandoom_backend.tag.dto.TagAssignmentResponse;
import com.example.fandoom_backend.tag.entity.TaggableType;

import java.util.List;

public interface TagAssignmentService {
    List<TagAssignmentResponse> listForTarget(TaggableType taggableType, Long taggableId);
    TagAssignmentResponse assign(Long tagId, TagAssignmentRequest request);
    void delete(Long id);
}
