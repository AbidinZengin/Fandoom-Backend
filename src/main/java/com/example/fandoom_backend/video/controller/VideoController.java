package com.example.fandoom_backend.video.controller;

import com.example.fandoom_backend.video.dto.VideoCreateRequest;
import com.example.fandoom_backend.video.dto.VideoResponse;
import com.example.fandoom_backend.video.dto.VideoUpdateRequest;
import com.example.fandoom_backend.video.entity.VideoItemType;
import com.example.fandoom_backend.video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Film ve dizi slug'ları çakışabildiği için okuma/ekleme uçları tür başına ayrı yoldadır;
// PUT/DELETE id ile tekildir. Yetki SecurityConfig'te (GET public, yazma EDITOR+).
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/movies/{slug}/videos")
    public List<VideoResponse> listForMovie(@PathVariable String slug) {
        return videoService.listByProduction(VideoItemType.MOVIE, slug);
    }

    @PostMapping("/movies/{slug}/videos")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse createForMovie(@PathVariable String slug, @Valid @RequestBody VideoCreateRequest request) {
        return videoService.create(VideoItemType.MOVIE, slug, request);
    }

    @GetMapping("/series/{slug}/videos")
    public List<VideoResponse> listForSeries(@PathVariable String slug) {
        return videoService.listByProduction(VideoItemType.SERIES, slug);
    }

    @PostMapping("/series/{slug}/videos")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse createForSeries(@PathVariable String slug, @Valid @RequestBody VideoCreateRequest request) {
        return videoService.create(VideoItemType.SERIES, slug, request);
    }

    @PutMapping("/videos/{id}")
    public VideoResponse update(@PathVariable Long id, @Valid @RequestBody VideoUpdateRequest request) {
        return videoService.update(id, request);
    }

    @DeleteMapping("/videos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        videoService.delete(id);
    }
}
