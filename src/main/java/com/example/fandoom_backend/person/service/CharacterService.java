package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import org.springframework.data.domain.Pageable;

public interface CharacterService {
    PageResponse<CharacterResponse> list(Pageable pageable);
    CharacterResponse getById(Long id);
    CharacterResponse getBySlug(String slug);
    CharacterResponse create(CharacterRequest request);
    CharacterResponse update(Long id, CharacterRequest request);
    void delete(Long id);
}
