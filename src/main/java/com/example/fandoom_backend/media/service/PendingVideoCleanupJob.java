package com.example.fandoom_backend.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Doğrudan yüklenip hiçbir içeriğe bağlanmamış ("pending" etiketli) videoları siler.
 *
 * <p>Güvenlik ağları: (1) yaşı {@code max-age} (varsayılan 24 saat) dolmamış hiçbir asset silinmez — kullanıcı
 * gönderisini yazıyor olabilir; en düşük değer 1 saattir, daha küçük yapılandırma açılışta reddedilir.
 * (2) {@code created_at} okunamazsa silinmez. (3) Silmeden hemen önce {@link MediaReferenceChecker}'lara sorulur:
 * pending etiketi kaldırılamamış ama bir içeriğe bağlı video silinmez, etiketi bu turda düzeltilir.
 * Çoklu instance'ta iş her instance'ta çalışır (silme idempotent, dağıtık kilit yok).
 */
@Slf4j
@Component
public class PendingVideoCleanupJob {

    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private static final int DELETE_BATCH = 100;
    static final Duration MIN_AGE = Duration.ofHours(1);

    private final Cloudinary cloudinary;
    private final List<MediaReferenceChecker> referenceCheckers;
    private final Duration maxAge;

    public PendingVideoCleanupJob(
            Cloudinary cloudinary,
            List<MediaReferenceChecker> referenceCheckers,
            @Value("${media.pending-cleanup.max-age-hours:24}") long maxAgeHours) {
        Duration configured = Duration.ofHours(maxAgeHours);
        if (configured.compareTo(MIN_AGE) < 0) {
            throw new IllegalArgumentException(
                    "media.pending-cleanup.max-age-hours en az 1 olmalı (yazılmakta olan gönderilerin dosyaları silinmesin)");
        }
        this.cloudinary = cloudinary;
        this.referenceCheckers = referenceCheckers;
        this.maxAge = configured;
    }

    @Scheduled(initialDelayString = "${media.pending-cleanup.initial-delay-ms:300000}",
            fixedDelayString = "${media.pending-cleanup.interval-ms:3600000}")
    public void run() {
        try {
            int deleted = cleanup(Instant.now());
            if (deleted > 0) {
                log.info("Yetim pending video temizliği: {} asset silindi", deleted);
            }
        } catch (Exception e) {
            // Sonraki çalışma baştan dener; işin düşmesi uygulamayı etkilemez.
            log.warn("Yetim pending video temizliği başarısız", e);
        }
    }

    int cleanup(Instant now) throws Exception {
        Instant cutoff = now.minus(maxAge);
        List<String> toDelete = new ArrayList<>();
        List<String> toUntag = new ArrayList<>();

        String cursor = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", CloudinaryDirectUpload.RESOURCE_TYPE, "max_results", PAGE_SIZE);
            if (cursor != null) {
                options.put("next_cursor", cursor);
            }
            Map<?, ?> response = cloudinary.api().resourcesByTag(CloudinaryDirectUpload.PENDING_TAG, options);
            if (response.get("resources") instanceof Collection<?> resources) {
                for (Object item : resources) {
                    if (item instanceof Map<?, ?> resource) {
                        classify(resource, cutoff, toDelete, toUntag);
                    }
                }
            }
            cursor = response.get("next_cursor") instanceof String next ? next : null;
            if (cursor == null) {
                break;
            }
        }

        untagQuietly(toUntag);
        return deleteInBatches(toDelete);
    }

    private void classify(Map<?, ?> resource, Instant cutoff, List<String> toDelete, List<String> toUntag) {
        Instant createdAt = createdAt(resource);
        // created_at okunamıyorsa emin olunamaz: silme. Cutoff'tan yeni olan (24 saatten genç) silinmez.
        if (createdAt == null || !createdAt.isBefore(cutoff)) {
            return;
        }
        if (!(resource.get("public_id") instanceof String publicId)) {
            return;
        }
        String secureUrl = resource.get("secure_url") instanceof String url ? url : null;
        if (secureUrl != null && referenceCheckers.stream().anyMatch(c -> c.isReferenced(secureUrl))) {
            toUntag.add(publicId);
        } else {
            toDelete.add(publicId);
        }
    }

    private static Instant createdAt(Map<?, ?> resource) {
        if (!(resource.get("created_at") instanceof String value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void untagQuietly(List<String> publicIds) {
        for (String publicId : publicIds) {
            try {
                cloudinary.uploader().removeTag(CloudinaryDirectUpload.PENDING_TAG, new String[]{publicId},
                        ObjectUtils.asMap("resource_type", CloudinaryDirectUpload.RESOURCE_TYPE));
            } catch (IOException | RuntimeException e) {
                log.warn("Kullanımdaki videonun pending etiketi düzeltilemedi: {}", publicId, e);
            }
        }
    }

    private int deleteInBatches(List<String> publicIds) {
        int deleted = 0;
        for (int from = 0; from < publicIds.size(); from += DELETE_BATCH) {
            List<String> batch = publicIds.subList(from, Math.min(from + DELETE_BATCH, publicIds.size()));
            try {
                cloudinary.api().deleteResources(batch,
                        ObjectUtils.asMap("resource_type", CloudinaryDirectUpload.RESOURCE_TYPE));
                deleted += batch.size();
            } catch (Exception e) {
                log.warn("Yetim video grubu silinemedi ({} adet), sonraki çalışmada tekrar denenecek", batch.size(), e);
            }
        }
        return deleted;
    }
}
