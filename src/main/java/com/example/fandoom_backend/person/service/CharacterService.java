package com.example.fandoom_backend.person.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.person.dto.CharacterRequest;
import com.example.fandoom_backend.person.dto.CharacterResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharacterService {
    PageResponse<CharacterResponse> list(Pageable pageable);
    List<CharacterResponse> listForMovie(Long movieId);
    List<CharacterResponse> listForSeries(Long seriesId);
    CharacterResponse getById(Long id);
    CharacterResponse getBySlug(String slug);
    CharacterResponse create(CharacterRequest request);
    CharacterResponse addToMovie(Long movieId, CharacterRequest request);
    List<CharacterResponse> addToMovieBatch(Long movieId, List<CharacterRequest> requests);
    CharacterResponse addToSeries(Long seriesId, CharacterRequest request);
    List<CharacterResponse> addToSeriesBatch(Long seriesId, List<CharacterRequest> requests);
    CharacterResponse update(Long id, CharacterRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
