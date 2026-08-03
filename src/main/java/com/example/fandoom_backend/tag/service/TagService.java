package com.example.fandoom_backend.tag.service;

import com.example.fandoom_backend.tag.dto.TagRequest;
import com.example.fandoom_backend.tag.dto.TagResponse;
import com.example.fandoom_backend.tag.entity.TagType;

import java.util.List;

public interface TagService {
    // type null ise tüm tag'ler döner, dolu ise yalnızca o TagType'a ait olanlar.
    List<TagResponse> list(TagType type);
    TagResponse getById(Long id);
    TagResponse create(TagRequest request);
    List<TagResponse> createBatch(List<TagRequest> requests);
    TagResponse update(Long id, TagRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
