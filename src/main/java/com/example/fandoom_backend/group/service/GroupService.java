package com.example.fandoom_backend.group.service;

import com.example.fandoom_backend.group.dto.GroupRequest;
import com.example.fandoom_backend.group.dto.GroupResponse;
import com.example.fandoom_backend.group.entity.GroupType;

import java.util.List;

public interface GroupService {
    List<GroupResponse> listForMovie(Long movieId, GroupType type);
    List<GroupResponse> listForSeries(Long seriesId, GroupType type);
    GroupResponse getById(Long id);
    GroupResponse getBySlug(String slug);
    GroupResponse addToMovie(Long movieId, GroupRequest request);
    GroupResponse addToSeries(Long seriesId, GroupRequest request);
    GroupResponse update(Long id, GroupRequest request);
    void delete(Long id);
    boolean existsById(Long id);
}
