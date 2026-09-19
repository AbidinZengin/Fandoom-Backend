package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.example.fandoom_backend.media.dto.VideoUploadSignatureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudinaryVideoUploadSigner implements VideoUploadSigner {

    private final Cloudinary cloudinary;
    private final String folder;
    private final int maxDurationSeconds;

    public CloudinaryVideoUploadSigner(
            Cloudinary cloudinary,
            @Value("${media.video.direct-upload-folder:community}") String folder,
            @Value("${media.video.max-duration-seconds:120}") int maxDurationSeconds) {
        this.cloudinary = cloudinary;
        this.folder = folder;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    @Override
    public VideoUploadSignatureResponse createSignature() {
        String cloudName = cloudinary.config.cloudName;
        String apiKey = cloudinary.config.apiKey;
        String apiSecret = cloudinary.config.apiSecret;
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new IllegalStateException("CLOUDINARY_URL tanımlı değil, video yükleme imzası üretilemiyor");
        }

        // İmzalanan her parametre istemci tarafından AYNEN gönderilmek zorundadır (klasör/etiket/format değiştirilemez).
        Map<String, String> params = new LinkedHashMap<>();
        params.put("allowed_formats", CloudinaryDirectUpload.ALLOWED_FORMATS);
        params.put("eager", CloudinaryDirectUpload.EAGER);
        params.put("eager_async", "true");
        params.put("folder", "fandoom/" + folder);
        params.put("tags", CloudinaryDirectUpload.PENDING_TAG);
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        String signature = cloudinary.apiSignRequest(new LinkedHashMap<String, Object>(params), apiSecret, 1);
        return new VideoUploadSignatureResponse(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/" + CloudinaryDirectUpload.RESOURCE_TYPE + "/upload",
                apiKey, signature, params, CloudinaryVideoStorageService.MAX_SIZE_BYTES, maxDurationSeconds);
    }
}
