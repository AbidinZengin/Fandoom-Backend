package com.example.fandoom_backend.media.service;

import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    MediaUploadResponse upload(MultipartFile file, String folder);

    void delete(String imageUrl);

    void deleteIfChanged(String oldImageUrl, String newImageUrl);
}
