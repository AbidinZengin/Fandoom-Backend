package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.repository.ThreadMediaRepository;
import com.example.fandoom_backend.media.service.MediaReferenceChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// media/'daki yetim temizlik işine "bu URL bir thread tarafından kullanılıyor mu" cevabını verir.
// Soft-delete edilmiş thread'lerin medyası da (satır durduğu için) kullanımda sayılır.
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadMediaReferenceChecker implements MediaReferenceChecker {

    private final ThreadMediaRepository threadMediaRepository;

    @Override
    public boolean isReferenced(String url) {
        return threadMediaRepository.existsByUrl(url);
    }
}
