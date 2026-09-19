package com.example.fandoom_backend.media.service;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Görsel ve video servislerinin ortak URL -> public_id çözümlemesi (Cloudinary destroy bunu ister).
final class CloudinaryPublicIds {

    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    private CloudinaryPublicIds() {
    }

    static String extract(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        Matcher matcher = PUBLIC_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
