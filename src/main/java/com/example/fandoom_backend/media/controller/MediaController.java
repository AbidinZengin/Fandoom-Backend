package com.example.fandoom_backend.media.controller;

import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import com.example.fandoom_backend.media.dto.VideoUploadSignatureResponse;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.VideoStorageService;
import com.example.fandoom_backend.media.service.VideoUploadSigner;
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
    private final VideoStorageService videoStorageService;
    private final VideoUploadSigner videoUploadSigner;

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

    @PostMapping(value = "/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "folder", defaultValue = "general") String folder) {
        return videoStorageService.upload(file, folder);
    }

    // İstemci videoyu doğrudan Cloudinary'ye yükler (sunucuya bayt uğramaz). Sıradan giriş yapmış kullanıcıya açık:
    // SecurityConfig'te /api/media/** rol kuralından ÖNCE ayrı listelenir, kullanıcı başına rate limit'lidir.
    @PostMapping("/videos/signature")
    public VideoUploadSignatureResponse videoUploadSignature() {
        return videoUploadSigner.createSignature();
    }

    @DeleteMapping("/videos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVideo(@RequestParam("url") String url) {
        videoStorageService.delete(url);
    }
}
