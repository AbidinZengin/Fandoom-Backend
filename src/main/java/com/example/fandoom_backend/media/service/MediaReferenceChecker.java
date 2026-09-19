package com.example.fandoom_backend.media.service;

// Yetim temizlik işinin, bir medya URL'inin hâlâ bir içerik tarafından kullanılıp kullanılmadığını sormasını sağlar.
// Arayüz media/'da tanımlı, uygulamaları içeriği tutan modüllerde (ör. community/) — media/ o modüllerin
// entity/repository'sine bağımlı olmaz (DIP, modül bağımsızlığı).
public interface MediaReferenceChecker {

    boolean isReferenced(String url);
}
