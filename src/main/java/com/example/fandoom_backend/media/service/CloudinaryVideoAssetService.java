package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.utils.ObjectUtils;
import com.example.fandoom_backend.common.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class CloudinaryVideoAssetService implements VideoAssetService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("mp4", "webm", "mov");

    private final Cloudinary cloudinary;
    private final int maxDurationSeconds;

    public CloudinaryVideoAssetService(
            Cloudinary cloudinary,
            @Value("${media.video.max-duration-seconds:120}") int maxDurationSeconds) {
        this.cloudinary = cloudinary;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    @Override
    public void verifyWithinLimits(String videoUrl) {
        String publicId = CloudinaryPublicIds.extract(videoUrl);
        if (publicId == null) {
            throw new InvalidFileException("Geçersiz video URL'i");
        }
        Map<?, ?> asset = fetch(publicId);

        long bytes = number(asset.get("bytes"), -1).longValue();
        double duration = number(asset.get("duration"), -1).doubleValue();
        Object format = asset.get("format");
        String violation = null;
        if (bytes < 0 || duration < 0) {
            violation = "Video boyutu/süresi okunamadı";
        } else if (bytes > CloudinaryVideoStorageService.MAX_SIZE_BYTES) {
            violation = "Video en fazla 50 MB olabilir";
        } else if (duration > maxDurationSeconds) {
            violation = "Video en fazla " + maxDurationSeconds + " saniye olabilir";
        } else if (format == null || !ALLOWED_FORMATS.contains(format.toString().toLowerCase())) {
            violation = "Desteklenmeyen video formatı: " + format;
        }
        if (violation != null) {
            // Limit dışı dosya storage'da yer/kota tutmasın. Yalnızca hâlâ "pending" ise silinir: başka bir
            // içeriğe bağlanmış (pending etiketi kalkmış) bir videoyu bu yoldan silmek mümkün olmamalı.
            if (hasPendingTag(asset)) {
                destroyQuietly(publicId);
            }
            throw new InvalidFileException(violation);
        }
    }

    @Override
    public void markAttached(String videoUrl) {
        String publicId = CloudinaryPublicIds.extract(videoUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().removeTag(CloudinaryDirectUpload.PENDING_TAG, new String[]{publicId},
                    ObjectUtils.asMap("resource_type", CloudinaryDirectUpload.RESOURCE_TYPE));
        } catch (IOException e) {
            throw new UncheckedIOException("Video pending etiketi kaldırılamadı: " + publicId, e);
        }
    }

    private Map<?, ?> fetch(String publicId) {
        try {
            return cloudinary.api().resource(publicId,
                    ObjectUtils.asMap("resource_type", CloudinaryDirectUpload.RESOURCE_TYPE));
        } catch (NotFound e) {
            throw new InvalidFileException("Video bulunamadı (yükleme tamamlanmamış olabilir): " + publicId);
        } catch (Exception e) {
            // Rate limit / ağ hatası dahil: doğrulanamayan video kabul edilmez (limit atlatma kapısı olmasın).
            throw new IllegalStateException("Video doğrulanamadı: " + publicId, e);
        }
    }

    private void destroyQuietly(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", CloudinaryDirectUpload.RESOURCE_TYPE));
        } catch (IOException | RuntimeException e) {
            log.warn("Limit dışı video silinemedi: {}", publicId, e);
        }
    }

    private static boolean hasPendingTag(Map<?, ?> asset) {
        return asset.get("tags") instanceof Collection<?> tags && tags.contains(CloudinaryDirectUpload.PENDING_TAG);
    }

    private static Number number(Object value, Number fallback) {
        return value instanceof Number n ? n : fallback;
    }
}
