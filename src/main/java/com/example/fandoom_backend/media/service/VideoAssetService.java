package com.example.fandoom_backend.media.service;

// Doğrudan yüklenmiş (imzalı) videoların yaşam döngüsü: bağlanmadan önce gerçek boyut/süre doğrulaması,
// bağlandıktan sonra "pending" işaretinin kaldırılması (yetim temizliğinden korunması).
public interface VideoAssetService {

    // Asset'in GERÇEK boyut/süre/formatını storage'dan okuyup limitlere karşı doğrular (istemcinin beyanına
    // güvenilmez). Limit aşımı InvalidFileException (400); aşan asset hâlâ "pending" ise hemen silinir.
    // Storage'a ulaşılamazsa doğrulanamayan video kabul edilmez (IllegalStateException, fail-closed).
    void verifyWithinLimits(String videoUrl);

    // Video bir içeriğe bağlandı: "pending" işaretini kaldırır. Çağıran commit sonrası çalıştırmalıdır.
    void markAttached(String videoUrl);
}
