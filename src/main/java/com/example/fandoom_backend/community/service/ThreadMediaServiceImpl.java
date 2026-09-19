package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.community.dto.ThreadMediaRequest;
import com.example.fandoom_backend.community.dto.ThreadMediaResponse;
import com.example.fandoom_backend.community.entity.Thread;
import com.example.fandoom_backend.community.entity.ThreadMedia;
import com.example.fandoom_backend.community.entity.ThreadMediaType;
import com.example.fandoom_backend.community.repository.ThreadMediaRepository;
import com.example.fandoom_backend.media.service.ImageStorageService;
import com.example.fandoom_backend.media.service.MediaUrlValidator;
import com.example.fandoom_backend.media.service.VideoAssetService;
import com.example.fandoom_backend.media.service.VideoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadMediaServiceImpl implements ThreadMediaService {

    static final int MAX_MEDIA_PER_THREAD = 6;
    // Thread medyası bu klasöre yüklenmişse (POST /api/media/*?folder=community) çıkarıldığında
    // storage'dan da silinir. Başka klasördeki (ör. film posteri) bir URL'i thread'e koyup sonra
    // çıkaran kullanıcının o varlığı silebilmesini önler.
    static final String DELETABLE_FOLDER = "community";

    private final ThreadMediaRepository threadMediaRepository;
    private final MediaUrlValidator mediaUrlValidator;
    private final ImageStorageService imageStorageService;
    private final VideoStorageService videoStorageService;
    private final VideoAssetService videoAssetService;

    @Override
    @Transactional
    public List<ThreadMediaResponse> replace(Thread thread, List<ThreadMediaRequest> requested) {
        validate(requested);

        List<ThreadMedia> existing = threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(thread.getId());
        Map<String, ThreadMediaType> previous = new LinkedHashMap<>();
        existing.forEach(m -> previous.putIfAbsent(m.getUrl(), m.getType()));
        // media tablosu öncesi yaratılmış thread'lerde tek görsel yalnızca thread.imageUrl'de durur
        if (StringUtils.hasText(thread.getImageUrl())) {
            previous.putIfAbsent(thread.getImageUrl(), ThreadMediaType.IMAGE);
        }

        // Bu thread'e YENİ eklenen videolar: gerçek boyut/süre storage'dan doğrulanır (istemci beyanına güvenilmez,
        // imzalı doğrudan yüklemede sunucu baytları görmez). Zaten bağlı videolar tekrar doğrulanmaz (Admin API kotası).
        List<String> newVideoUrls = requested.stream()
                .filter(m -> m.type() == ThreadMediaType.VIDEO && !previous.containsKey(m.url()))
                .map(ThreadMediaRequest::url)
                .distinct()
                .toList();
        newVideoUrls.forEach(videoAssetService::verifyWithinLimits);

        threadMediaRepository.deleteAll(existing);
        List<ThreadMedia> saved = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            ThreadMediaRequest item = requested.get(i);
            saved.add(ThreadMedia.builder().thread(thread).type(item.type()).url(item.url()).position(i).build());
        }
        threadMediaRepository.saveAll(saved);

        // imageUrl = ilk IMAGE'ın url'i: eski istemciler için geriye uyumlu, ayrıca eski değeri temizler
        thread.setImageUrl(saved.stream()
                .filter(m -> m.getType() == ThreadMediaType.IMAGE)
                .map(ThreadMedia::getUrl)
                .findFirst()
                .orElse(null));

        Set<String> kept = requested.stream().map(ThreadMediaRequest::url).collect(Collectors.toSet());
        previous.keySet().removeIf(kept::contains);
        deleteFromStorageAfterCommit(unreferenced(previous));
        markVideosAttachedAfterCommit(newVideoUrls);

        return saved.stream().map(ThreadMediaServiceImpl::toResponse).toList();
    }

    @Override
    public List<ThreadMediaResponse> getMedia(Thread thread) {
        List<ThreadMediaResponse> rows = threadMediaRepository.findByThread_IdOrderByPositionAscIdAsc(thread.getId())
                .stream().map(ThreadMediaServiceImpl::toResponse).toList();
        return rows.isEmpty() ? legacyMedia(thread) : rows;
    }

    @Override
    public Map<Long, List<ThreadMediaResponse>> getMediaByThread(List<Thread> threads) {
        if (threads.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = threads.stream().map(Thread::getId).toList();
        Map<Long, List<ThreadMediaResponse>> byThread = new HashMap<>();
        for (ThreadMedia media : threadMediaRepository.findByThread_IdInOrderByPositionAscIdAsc(ids)) {
            byThread.computeIfAbsent(media.getThread().getId(), k -> new ArrayList<>()).add(toResponse(media));
        }
        Map<Long, List<ThreadMediaResponse>> result = new HashMap<>();
        for (Thread thread : threads) {
            List<ThreadMediaResponse> rows = byThread.get(thread.getId());
            result.put(thread.getId(), rows != null ? rows : legacyMedia(thread));
        }
        return result;
    }

    private void validate(List<ThreadMediaRequest> requested) {
        if (requested.size() > MAX_MEDIA_PER_THREAD) {
            throw new InvalidReferenceException("Bir thread'e en fazla " + MAX_MEDIA_PER_THREAD + " medya eklenebilir");
        }
        for (ThreadMediaRequest item : requested) {
            boolean image = item.type() == ThreadMediaType.IMAGE;
            boolean valid = image ? mediaUrlValidator.isOwnedImage(item.url()) : mediaUrlValidator.isOwnedVideo(item.url());
            if (!valid) {
                throw new InvalidReferenceException("Geçersiz " + (image ? "görsel" : "video") + " URL'i: " + item.url());
            }
        }
    }

    // Başka bir thread'in hâlâ kullandığı ya da izinli klasör dışındaki URL'leri silme listesinden eler.
    // Transaction içinde çalışır: sorgu öncesi bekleyen değişiklikler flush edilir. Medya tablosu
    // öncesi legacy thread.imageUrl'ler ayrıca çapraz kontrol edilmez (image_url indeksli değil);
    // onlar zaten "community" klasöründe değildir, klasör kontrolü ile dışarıda kalır.
    private Map<String, ThreadMediaType> unreferenced(Map<String, ThreadMediaType> removed) {
        Map<String, ThreadMediaType> deletable = new LinkedHashMap<>();
        removed.forEach((url, type) -> {
            if (mediaUrlValidator.isInFolder(url, DELETABLE_FOLDER)
                    && !threadMediaRepository.existsByUrl(url)) {
                deletable.put(url, type);
            }
        });
        return deletable;
    }

    // Storage silmesi geri alınamaz: DB transaction'ı rollback olursa dosya kaybolmasın diye commit
    // sonrasına bırakılır. Silme hatası kullanıcı isteğini bozmaz (yalnızca orphan bırakır, loglanır).
    private void deleteFromStorageAfterCommit(Map<String, ThreadMediaType> urls) {
        if (urls.isEmpty()) {
            return;
        }
        Runnable cleanup = () -> urls.forEach((url, type) -> {
            try {
                if (type == ThreadMediaType.VIDEO) {
                    videoStorageService.delete(url);
                } else {
                    imageStorageService.delete(url);
                }
            } catch (RuntimeException e) {
                log.warn("Thread medyası storage'dan silinemedi: {}", url, e);
            }
        });
        runAfterCommit(cleanup);
    }

    // Bağlanan videoların "pending" işareti kalkar (yetim temizliği silmesin). Commit sonrası: rollback olursa
    // video pending kalır ve 24 saat sonra temizlenir. Etiket kaldırma başarısız olursa zararsızdır: temizlik işi
    // silmeden önce ThreadMediaReferenceChecker ile kullanımı kontrol eder.
    private void markVideosAttachedAfterCommit(List<String> videoUrls) {
        if (videoUrls.isEmpty()) {
            return;
        }
        runAfterCommit(() -> videoUrls.forEach(url -> {
            try {
                videoAssetService.markAttached(url);
            } catch (RuntimeException e) {
                log.warn("Video pending etiketi kaldırılamadı: {}", url, e);
            }
        }));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private List<ThreadMediaResponse> legacyMedia(Thread thread) {
        if (!StringUtils.hasText(thread.getImageUrl())) {
            return Collections.emptyList();
        }
        return List.of(new ThreadMediaResponse(ThreadMediaType.IMAGE, thread.getImageUrl(), 0));
    }

    private static ThreadMediaResponse toResponse(ThreadMedia media) {
        return new ThreadMediaResponse(media.getType(), media.getUrl(), media.getPosition());
    }
}
