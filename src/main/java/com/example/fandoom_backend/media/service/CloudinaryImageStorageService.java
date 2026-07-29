package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import com.example.fandoom_backend.media.dto.MediaUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    private final Cloudinary cloudinary;
    private final ImageCompressor imageCompressor;

    @Override
    public MediaUploadResponse upload(MultipartFile file, String folder) {
        validate(file);
        try {
            byte[] compressed = imageCompressor.compress(file.getBytes());
            Map<?, ?> result = cloudinary.uploader().upload(compressed, ObjectUtils.asMap(
                    "folder", "fandoom/" + folder,
                    "resource_type", "image"));
            return new MediaUploadResponse((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (IOException e) {
            throw new UncheckedIOException("Görsel Cloudinary'e yüklenemedi", e);
        }
    }

    @Override
    public void delete(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new UncheckedIOException("Görsel Cloudinary'den silinemedi: " + publicId, e);
        }
    }

    @Override
    public void deleteIfChanged(String oldImageUrl, String newImageUrl) {
        if (StringUtils.hasText(oldImageUrl) && !oldImageUrl.equals(newImageUrl)) {
            delete(oldImageUrl);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Yüklenecek dosya boş olamaz");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("Desteklenmeyen dosya tipi: " + file.getContentType());
        }
    }

    private String extractPublicId(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        Matcher matcher = PUBLIC_ID_PATTERN.matcher(imageUrl);
        return matcher.find() ? matcher.group(1) : null;
    }
}
