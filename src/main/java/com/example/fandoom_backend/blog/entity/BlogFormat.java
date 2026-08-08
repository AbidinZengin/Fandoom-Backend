package com.example.fandoom_backend.blog.entity;

// Blog hub filtre panelinin Format seçeneği. Sabit/kararlı bir liste olduğu
// için tag/ modülündeki serbest Tag kaydı yerine bilinçli olarak enum
// seçildi (bkz. MOOD ile karşılaştırma: MOOD'un aksine format'ın niş/sık
// büyüyen bir liste olması beklenmiyor). Blog başına tek değer taşır.
public enum BlogFormat {
    REVIEW, RECAP, ANALYSIS, RANKING, INTERVIEW, BEHIND_THE_SCENES , CHARACTER
}
