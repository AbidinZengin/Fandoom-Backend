package com.example.fandoom_backend.media.service;

import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VideoStorageService {

    MediaUploadResponse upload(MultipartFile file, String folder);

    void delete(String videoUrl);
}
