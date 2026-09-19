package com.example.fandoom_backend.media.dto;

import java.util.Map;

// Frontend, videoyu doğrudan uploadUrl'e multipart POST eder: form alanları = params'ın TÜMÜ (aynen) +
// api_key + signature + file. params imzaya dahil olduğundan değiştirilirse Cloudinary yüklemeyi reddeder.
// İmza (timestamp'ten itibaren) 1 saat geçerlidir. Limitler yalnızca istemci tarafı ön kontrol içindir;
// bağlayıcı doğrulama thread'e eklenirken yapılır (Cloudinary imzalı yüklemede istek başına boyut sınırı vermez).
public record VideoUploadSignatureResponse(
        String uploadUrl,
        String apiKey,
        String signature,
        Map<String, String> params,
        long maxFileSizeBytes,
        int maxDurationSeconds) {
}
