package com.example.fandoom_backend.account.service;

// Takip servisinin "bu profil gizli mi" sorusu. Arayüz account/'ta, uygulaması community/'de
// (profil verisi orada): account/ community/'ye derleme zamanı bağlanmaz ve Spring'de döngü doğmaz,
// çünkü uygulayan bean yalnızca UserProfileRepository'ye bağlı (ProfileFollowService'e değil).
public interface ProfilePrivacyLookup {
    boolean isPrivate(Long userId);
}
