package com.example.fandoom_backend.media.service;

// Doğrudan (imzalı) video yüklemesinin sabitleri: imza, doğrulama ve temizlik işi aynı değerlere dayanır.
final class CloudinaryDirectUpload {

    // Yükleme anında eklenir; içeriğe bağlanınca kaldırılır. Etiketi hâlâ taşıyan eski asset = yetim adayı.
    static final String PENDING_TAG = "pending";
    static final String RESOURCE_TYPE = "video";
    static final String ALLOWED_FORMATS = "mp4,webm,mov";
    // Optimize mp4 türevi yükleme cevabını bekletmeden (eager_async) hazırlanır; ilk oynatma gecikmesi olmaz.
    static final String EAGER = "q_auto,f_mp4";

    private CloudinaryDirectUpload() {
    }
}
