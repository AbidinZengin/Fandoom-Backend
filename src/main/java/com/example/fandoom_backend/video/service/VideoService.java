package com.example.fandoom_backend.video.service;

import com.example.fandoom_backend.video.dto.VideoCreateRequest;
import com.example.fandoom_backend.video.dto.VideoResponse;
import com.example.fandoom_backend.video.dto.VideoUpdateRequest;
import com.example.fandoom_backend.video.entity.VideoItemType;

import java.util.List;

public interface VideoService {
    // Yapım bulunamazsa ResourceNotFoundException (404); video yoksa boş liste.
    List<VideoResponse> listByProduction(VideoItemType itemType, String slug);
    VideoResponse create(VideoItemType itemType, String slug, VideoCreateRequest request);
    VideoResponse update(Long id, VideoUpdateRequest request);
    void delete(Long id);
}
