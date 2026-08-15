package com.example.fandoom_backend.lore.service;

import com.example.fandoom_backend.lore.dto.GroupRequest;
import com.example.fandoom_backend.lore.dto.GroupResponse;

import java.util.List;

public interface GroupService {
    List<GroupResponse> listForMovie(Long movieId, Long categoryId);
    List<GroupResponse> listForSeries(Long seriesId, Long categoryId);
    GroupResponse getById(Long id);
    GroupResponse getBySlug(String slug);
    GroupResponse addToMovie(Long movieId, GroupRequest request);
    GroupResponse addToSeries(Long seriesId, GroupRequest request);
    GroupResponse update(Long id, GroupRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
