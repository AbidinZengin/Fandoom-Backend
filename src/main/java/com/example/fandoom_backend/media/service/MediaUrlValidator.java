package com.example.fandoom_backend.media.service;

// Başka modüllerden gelen, client'ın gönderdiği medya URL'lerinin bu projenin storage hesabına ait
// olduğunu doğrular (rastgele hotlink'i engellemek için). Sağlayıcıya özgü URL bilgisi media/ içinde kalır.
public interface MediaUrlValidator {

    boolean isOwnedImage(String url);

    boolean isOwnedVideo(String url);

    // url, "fandoom/<folder>/" altına yüklenmiş bir varlığa mı işaret ediyor (upload'daki folder parametresi).
    boolean isInFolder(String url, String folder);
}
