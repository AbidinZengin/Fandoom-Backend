package com.example.fandoom_backend.media.controller;

import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import com.example.fandoom_backend.media.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "folder", defaultValue = "general") String folder) {
        return imageStorageService.upload(file, folder);
    }

    @DeleteMapping("/images")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam("url") String url) {
        imageStorageService.delete(url);
    }
}
