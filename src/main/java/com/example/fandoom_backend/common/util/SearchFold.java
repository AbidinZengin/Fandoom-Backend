package com.example.fandoom_backend.common.util;

// Arama için Türkçe noktasız "ı" katlaması. Kolon collation'ı (utf8mb4_unicode_ci) I=i=İ, ü=u, ş=s eşler ama "ı"yı
// AYRI harf sayar: "ışık" / "IŞIK" aramaları "Işık"ı bulamaz. Çözüm: iki tarafta da ı -> i.
//   - Girdi tarafı (Java): fold(query)
//   - Kolon tarafı (JPQL): SQL_OPEN + "m.title" + SQL_CLOSE  ->  replace(m.title, 'ı', 'i')
// İkisi bir çifttir (LikePatterns'taki kaçış karakteri/ESCAPE gibi); biri değişirse diğeri de değişmeli.
// Denendi (2026-10-15, gerçek MySQL): "Işık Yolu" -> ışık/IŞIK/isik/Işık hepsi eşleşir; İngilizce I/i bozulmaz.
// Bedeli: her satırda replace() — tam tarama zaten yapıldığı için bugün ihmal edilebilir; ölçek sorunu olursa ayrı
// "aranabilir başlık" kolonu (tasarım belgesi, Karar 6 büyüme merdiveni) düşünülür.
public final class SearchFold {

    public static final String SQL_OPEN = "replace(";
    public static final String SQL_CLOSE = ", 'ı', 'i')";

    private SearchFold() {
    }

    public static String fold(String text) {
        return text.replace('ı', 'i');
    }
}
