package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.tag.dto.TagRequest;
import com.example.fandoom_backend.tag.dto.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> list();
    TagResponse getById(Long id);
    TagResponse create(TagRequest request);
    List<TagResponse> createBatch(List<TagRequest> requests);
    TagResponse update(Long id, TagRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
