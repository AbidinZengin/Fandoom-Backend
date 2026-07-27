package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharacterService {
    PageResponse<CharacterResponse> list(Pageable pageable);
    CharacterResponse getById(Long id);
    CharacterResponse getBySlug(String slug);
    CharacterResponse create(CharacterRequest request);
    List<CharacterResponse> createBatch(List<CharacterRequest> requests);
    CharacterResponse update(Long id, CharacterRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
