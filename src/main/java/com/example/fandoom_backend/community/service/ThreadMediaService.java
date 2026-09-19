package com.example.fandoom_backend.community.service;

import com.example.fandoom_backend.community.dto.ThreadMediaRequest;
import com.example.fandoom_backend.community.dto.ThreadMediaResponse;
import com.example.fandoom_backend.community.entity.Thread;

import java.util.List;
import java.util.Map;

// Thread medyasının (görsel/video) tek sahibi: doğrulama, kalıcılık, Cloudinary temizliği ve
// okuma. ThreadServiceImpl'i medya detayından ayırmak için ayrı servis (SRP).
public interface ThreadMediaService {

    // Thread'in medyasını TAMAMEN değiştirir (boş liste = hepsi silinir). Doğrulama hatası
    // InvalidReferenceException (400). Çıkarılan medya, transaction commit'inden sonra storage'dan silinir.
    List<ThreadMediaResponse> replace(Thread thread, List<ThreadMediaRequest> requested);

    List<ThreadMediaResponse> getMedia(Thread thread);

    // Liste sayfası için toplu okuma (tek sorgu). Her thread için bir giriş döner (medyasızsa boş liste).
    Map<Long, List<ThreadMediaResponse>> getMediaByThread(List<Thread> threads);
}
