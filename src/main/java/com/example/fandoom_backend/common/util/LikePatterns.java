package com.example.fandoom_backend.common.util;

import java.util.Objects;

// Kullanıcı metninden güvenli LIKE kalıbı üretir: '%' ve '_' joker değil düz karakter olarak aranır.
// Sorgu tarafında kalıp HER ZAMAN "LIKE :pattern ESCAPE '!'" ile kullanılmalı — kaçış karakteri ile ESCAPE ifadesi bir çifttir.
// '!' seçildi çünkü '\' MySQL string literal'inde zaten kaçış karakteri.
// Trim/uzunluk/küçük harf burada YAPILMAZ: bunlar çağıranın girdi kuralıdır (DB collation zaten harfe duyarsız).
public final class LikePatterns {

    public static final char ESCAPE_CHAR = '!';
    // @Query metinlerinde kullanılır ("m.title LIKE :pattern" + ESCAPE): kaçış karakteri ile çift tek yerden gelir.
    public static final String ESCAPE = " ESCAPE '" + ESCAPE_CHAR + "'";

    private LikePatterns() {
    }

    // "içinde geçen": dune -> %dune%
    public static String contains(String text) {
        return "%" + escape(text) + "%";
    }

    // "ile başlayan": dune -> dune%
    public static String startsWith(String text) {
        return escape(text) + "%";
    }

    static String escape(String text) {
        Objects.requireNonNull(text, "text");
        StringBuilder sb = new StringBuilder(text.length() + 4);
        for (char c : text.toCharArray()) {
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                sb.append(ESCAPE_CHAR);
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
