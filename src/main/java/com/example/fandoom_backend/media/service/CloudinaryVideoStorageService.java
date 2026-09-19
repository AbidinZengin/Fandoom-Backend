package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryVideoStorageService implements VideoStorageService {

    static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    private final Cloudinary cloudinary;

    @Override
    public MediaUploadResponse upload(MultipartFile file, String folder) {
        validate(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "fandoom/" + folder,
                    "resource_type", "video"));
            return new MediaUploadResponse((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (IOException e) {
            throw new UncheckedIOException("Video Cloudinary'e yüklenemedi", e);
        }
    }

    @Override
    public void delete(String videoUrl) {
        String publicId = CloudinaryPublicIds.extract(videoUrl);
        if (publicId == null) {
            return;
        }
        try {
            // resource_type verilmezse Cloudinary varsayılan "image" ile arar ve video silinmez
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
        } catch (IOException e) {
            throw new UncheckedIOException("Video Cloudinary'den silinemedi: " + publicId, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Yüklenecek dosya boş olamaz");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("Desteklenmeyen dosya tipi: " + file.getContentType());
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("Video en fazla 50 MB olabilir");
        }
    }
}
